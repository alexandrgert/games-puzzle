package ru.alexandrgert.gamespuzzle.domain

import kotlin.math.abs

object BoardEngine {
    private val orthogonalDirections = listOf(
        Cell(-1, 0),
        Cell(1, 0),
        Cell(0, -1),
        Cell(0, 1),
    )

    fun identityUnlocked(size: GridSize): Board {
        val n = size.n
        val count = n * n
        return Board(size, IntArray(count) { it }, BooleanArray(count))
    }

    fun isWin(board: Board): Boolean {
        for (i in board.tiles.indices) {
            if (board.tiles[i] != i || !board.locked[i]) return false
        }
        return true
    }

    fun hasResultativeSwap(board: Board): Boolean {
        val n = board.n
        val cells = mutableListOf<Cell>()
        for (r in 0 until n) {
            for (c in 0 until n) {
                val cell = Cell(r, c)
                if (!board.isLockedCell(cell)) cells.add(cell)
            }
        }
        for (i in cells.indices) {
            for (j in i + 1 until cells.size) {
                val result = trySwap(board, cells[i], cells[j])
                if (result is MoveResult.Applied && result.joined) return true
            }
        }
        return false
    }

    fun shuffle(size: GridSize, random: java.util.Random): Board {
        val n = size.n
        val count = n * n
        repeat(500) {
            val tiles = IntArray(count) { it }
            for (i in count - 1 downTo 1) {
                val j = random.nextInt(i + 1)
                val tmp = tiles[i]
                tiles[i] = tiles[j]
                tiles[j] = tmp
            }
            if (tiles.toList() != (0 until count).toList()) {
                val board = Board(size, tiles, BooleanArray(count))
                if (hasResultativeSwap(board)) return board
            }
        }
        error("shuffle failed to find a playable permutation")
    }

    fun trySwap(board: Board, a: Cell, b: Cell): MoveResult {
        val n = board.n
        if (a == b || !a.inBounds(n) || !b.inBounds(n)) return MoveResult.Reverted(board)
        if (board.isLockedCell(a) || board.isLockedCell(b)) return MoveResult.Reverted(board)

        val swappedTiles = board.tiles.copyOf()
        val ia = a.index(n)
        val ib = b.index(n)
        swappedTiles[ia] = board.tiles[ib]
        swappedTiles[ib] = board.tiles[ia]
        val swapped = Board(board.size, swappedTiles, board.locked)

        val tileA = swapped.tileAt(a)
        val tileB = swapped.tileAt(b)
        if (isCorrectJoin(n, a, tileA, b, tileB)) {
            val snapped = snapPairToHome(swapped, tileA, tileB)
                ?: return MoveResult.Reverted(board)
            return appliedAfterLocks(swapped, snapped)
        }

        val neighbourJoin = findUnlockedNeighbourJoin(swapped, a, b)
        if (neighbourJoin != null) {
            val snapped = snapPairToHome(swapped, neighbourJoin.first, neighbourJoin.second)
                ?: return MoveResult.Reverted(board)
            return appliedAfterLocks(swapped, snapped)
        }

        val nextLocked = swapped.locked.copyOf()
        for (t in listOf(tileA, tileB)) {
            if (lockIfHomeAgainstLocked(swapped, t)) {
                nextLocked[t] = true
            }
        }
        val attached = Board(board.size, swapped.tiles, nextLocked)
        return appliedAfterLocks(swapped, attached)
    }

    private fun appliedAfterLocks(beforeLocks: Board, afterDirectLocks: Board): MoveResult.Applied {
        val cascaded = cascadeHomeLocks(afterDirectLocks)
        val joined = !cascaded.locked.contentEquals(beforeLocks.locked)
        return MoveResult.Applied(cascaded, joined)
    }

    private fun cascadeHomeLocks(board: Board): Board {
        val nextLocked = board.locked.copyOf()
        var changed = true
        while (changed) {
            changed = false
            for (tileId in nextLocked.indices) {
                if (nextLocked[tileId]) continue
                if (board.tiles[tileId] != tileId) continue
                if (!joinsLockedNeighbour(board.n, board.tiles, nextLocked, tileId)) continue
                nextLocked[tileId] = true
                changed = true
            }
        }
        if (nextLocked.contentEquals(board.locked)) return board
        return Board(board.size, board.tiles, nextLocked)
    }

    private fun findUnlockedNeighbourJoin(board: Board, a: Cell, b: Cell): Pair<Int, Int>? {
        val n = board.n
        for (cell in listOf(a, b)) {
            val tile = board.tileAt(cell)
            if (board.isLockedTile(tile)) continue
            for (direction in orthogonalDirections) {
                val neighbor = Cell(cell.row + direction.row, cell.col + direction.col)
                if (!neighbor.inBounds(n)) continue
                val neighborTile = board.tileAt(neighbor)
                if (board.isLockedTile(neighborTile)) continue
                if (isCorrectJoin(n, cell, tile, neighbor, neighborTile)) {
                    return tile to neighborTile
                }
            }
        }
        return null
    }

    private fun lockIfHomeAgainstLocked(board: Board, tileId: Int): Boolean {
        if (board.tiles[tileId] != tileId) return false
        return joinsLockedNeighbour(board.n, board.tiles, board.locked, tileId)
    }

    private fun joinsLockedNeighbour(
        n: Int,
        tiles: IntArray,
        locked: BooleanArray,
        tileId: Int,
    ): Boolean {
        val home = Cell.fromIndex(tileId, n)
        for (direction in orthogonalDirections) {
            val neighbor = Cell(home.row + direction.row, home.col + direction.col)
            if (!neighbor.inBounds(n)) continue
            val neighborTile = tiles[neighbor.index(n)]
            if (!locked[neighborTile]) continue
            if (isCorrectJoin(n, home, tileId, neighbor, neighborTile)) return true
        }
        return false
    }

    private fun isCorrectJoin(
        n: Int,
        cellA: Cell,
        tileA: Int,
        cellB: Cell,
        tileB: Int,
    ): Boolean {
        val boardRowOffset = cellB.row - cellA.row
        val boardColOffset = cellB.col - cellA.col
        val isOrthogonal =
            (abs(boardRowOffset) == 1 && boardColOffset == 0) ||
                (boardRowOffset == 0 && abs(boardColOffset) == 1)
        if (!isOrthogonal) return false

        val homeA = Cell.fromIndex(tileA, n)
        val homeB = Cell.fromIndex(tileB, n)
        return homeB.row - homeA.row == boardRowOffset &&
            homeB.col - homeA.col == boardColOffset
    }

    private fun snapPairToHome(board: Board, tile0: Int, tile1: Int): Board? {
        val home0 = tile0
        val home1 = tile1
        val occupant0 = board.tiles[home0]
        val occupant1 = board.tiles[home1]
        if (occupant0 != tile0 && board.isLockedTile(occupant0)) return null
        if (occupant1 != tile1 && board.isLockedTile(occupant1)) return null

        val position0 = board.tiles.indexOf(tile0)
        val position1 = board.tiles.indexOf(tile1)
        val leftoverSlots = listOf(position0, position1).filter { it != home0 && it != home1 }
        val leftoverTiles = buildList {
            if (occupant0 != tile0 && occupant0 != tile1) add(occupant0)
            if (occupant1 != tile0 && occupant1 != tile1) add(occupant1)
        }
        if (leftoverSlots.size != leftoverTiles.size) return null

        val nextTiles = board.tiles.copyOf()
        nextTiles[home0] = tile0
        nextTiles[home1] = tile1
        leftoverSlots.zip(leftoverTiles).forEach { (slot, tile) ->
            nextTiles[slot] = tile
        }

        val nextLocked = board.locked.copyOf()
        nextLocked[tile0] = true
        nextLocked[tile1] = true
        return Board(board.size, nextTiles, nextLocked)
    }
}
