package ru.alexandrgert.gamespuzzle.domain

import java.util.Random

class PlaySession(
    val size: GridSize,
    val statsEnabled: Boolean,
    random: Random,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val startedAtMs = currentTimeMillis()
    private var wonAtMs: Long? = null

    var board: Board = BoardEngine.shuffle(size, random)
        private set
    var selected: Cell? = null
        private set
    var moves: Int = 0
        private set
    var peek: Boolean = false
        private set
    val elapsedMs: Long
        get() = ((wonAtMs ?: currentTimeMillis()) - startedAtMs).coerceAtLeast(0L)

    fun tap(cell: Cell): MoveResult? {
        if (!cell.inBounds(size.n) || board.isLockedCell(cell)) return null

        val first = selected
        if (first == null) {
            selected = cell
            return null
        }
        if (first == cell) {
            selected = null
            return null
        }

        return swap(first, cell)
    }

    fun swap(a: Cell, b: Cell): MoveResult? {
        if (a == b || !a.inBounds(size.n) || !b.inBounds(size.n)) return null

        selected = null
        val result = BoardEngine.trySwap(board, a, b)
        when (result) {
            is MoveResult.Applied -> {
                board = result.board
                moves++
                if (isWin() && wonAtMs == null) wonAtMs = currentTimeMillis()
            }
            is MoveResult.Reverted -> Unit
        }
        return result
    }

    fun tileShownAt(cell: Cell): Int = board.tileAt(cell)

    fun togglePeek() {
        peek = !peek
    }

    fun isWin(): Boolean = BoardEngine.isWin(board)
}
