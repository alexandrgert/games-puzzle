package ru.alexandrgert.gamespuzzle.domain

class Board(
    val size: GridSize,
    tiles: IntArray,
    locked: BooleanArray,
) {
    val n: Int = size.n
    val tiles: IntArray = tiles.copyOf()
    val locked: BooleanArray = locked.copyOf()

    init {
        require(this.tiles.size == n * n)
        require(this.locked.size == n * n)

        val seen = BooleanArray(n * n)
        for (slot in tiles.indices) {
            val tileId = tiles[slot]
            require(tileId in tiles.indices) {
                "tile id $tileId at slot $slot is out of range"
            }
            require(!seen[tileId]) { "duplicate tile id $tileId" }
            seen[tileId] = true
        }

        for (tileId in locked.indices) {
            if (!locked[tileId]) continue
            require(tiles[tileId] == tileId) {
                "locked tile $tileId must be at home slot $tileId"
            }
        }
    }

    fun tileAt(cell: Cell): Int = tiles[cell.index(n)]

    fun isLockedTile(tileId: Int): Boolean = locked[tileId]

    fun isLockedCell(cell: Cell): Boolean = locked[tileAt(cell)]

    fun copy(): Board = Board(size, tiles, locked)
}

sealed class MoveResult {
    data class Applied(val board: Board) : MoveResult()
    data class Reverted(val board: Board) : MoveResult()
}
