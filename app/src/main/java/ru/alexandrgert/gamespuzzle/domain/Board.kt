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
