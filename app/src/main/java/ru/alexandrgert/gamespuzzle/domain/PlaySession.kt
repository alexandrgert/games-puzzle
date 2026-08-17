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
    val elapsedMs: Long
        get() = ((wonAtMs ?: currentTimeMillis()) - startedAtMs).coerceAtLeast(0L)

    fun tap(cell: Cell): MoveResult? {
        if (!cell.inBounds(size.n) || board.isLockedCell(cell)) return null

        val first = selected
        if (first == null) {
            selected = cell
            lastReverted = false
            return null
        }
        if (first == cell) {
            selected = null
            lastReverted = false
            return null
        }

        val result = BoardEngine.trySwap(board, first, cell)
        selected = null
        when (result) {
            is MoveResult.Applied -> {
                board = result.board
                if (statsEnabled) moves++
                if (isWin() && wonAtMs == null) wonAtMs = currentTimeMillis()
                lastReverted = false
            }
            is MoveResult.Reverted -> {
                lastReverted = true
            }
        }
        return result
    }

    fun togglePeek() {
        peek = !peek
    }

    fun clearLastReverted() {
        lastReverted = false
    }

    fun isWin(): Boolean = BoardEngine.isWin(board)
}
