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
    var lastReverted: Boolean = false
        private set
    var revertedA: Cell? = null
        private set
    var revertedB: Cell? = null
        private set
    val elapsedMs: Long
        get() = ((wonAtMs ?: currentTimeMillis()) - startedAtMs).coerceAtLeast(0L)

    fun tap(cell: Cell): MoveResult? {
        if (!cell.inBounds(size.n) || board.isLockedCell(cell)) return null

        val first = selected
        if (first == null) {
            selected = cell
            lastReverted = false
            revertedA = null
            revertedB = null
            return null
        }
        if (first == cell) {
            selected = null
            lastReverted = false
            revertedA = null
            revertedB = null
            return null
        }

        return swap(first, cell)
    }

    fun swap(a: Cell, b: Cell): MoveResult? {
        if (a == b || !a.inBounds(size.n) || !b.inBounds(size.n)) return null
        if (board.isLockedCell(a) || board.isLockedCell(b)) return null

        selected = null
        val result = BoardEngine.trySwap(board, a, b)
        when (result) {
            is MoveResult.Applied -> {
                board = result.board
                if (statsEnabled) moves++
                if (isWin() && wonAtMs == null) wonAtMs = currentTimeMillis()
                lastReverted = false
                revertedA = null
                revertedB = null
            }
            is MoveResult.Reverted -> {
                lastReverted = true
                revertedA = a
                revertedB = b
            }
        }
        return result
    }

    fun tileShownAt(cell: Cell): Int {
        val showAttempt = lastReverted && revertedA != null && revertedB != null
        return when {
            showAttempt && cell == revertedA -> board.tileAt(revertedB!!)
            showAttempt && cell == revertedB -> board.tileAt(revertedA!!)
            else -> board.tileAt(cell)
        }
    }

    fun togglePeek() {
        peek = !peek
    }

    fun clearLastReverted() {
        lastReverted = false
        revertedA = null
        revertedB = null
    }

    fun isWin(): Boolean = BoardEngine.isWin(board)
}
