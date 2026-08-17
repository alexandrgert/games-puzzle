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

        for (cell in listOf(a, b)) {
            for (direction in orthogonalDirections) {
                val neighbor = Cell(cell.row + direction.row, cell.col + direction.col)
                if (!neighbor.inBounds(n)) continue

                val tile = swapped.tileAt(cell)
                val neighborTile = swapped.tileAt(neighbor)
                if (swapped.isLockedTile(tile) || swapped.isLockedTile(neighborTile)) continue
                if (!isCorrectJoin(n, cell, tile, neighbor, neighborTile)) continue

                val snapped = snapPairToHome(swapped, tile, neighborTile)
                    ?: return MoveResult.Reverted(board)
                return MoveResult.Applied(snapped)
            }
        }

        return MoveResult.Reverted(board)
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
