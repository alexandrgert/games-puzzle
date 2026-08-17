package ru.alexandrgert.gamespuzzle.ui.play

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.util.Random
import ru.alexandrgert.gamespuzzle.domain.Board
import ru.alexandrgert.gamespuzzle.domain.Cell
import ru.alexandrgert.gamespuzzle.domain.GridSize
import ru.alexandrgert.gamespuzzle.domain.PlaySession

data class PlayState(
    val board: Board,
    val selected: Cell?,
    val elapsedMs: Long,
    val moves: Int,
    val won: Boolean,
    val peek: Boolean,
    val lastReverted: Boolean,
)

class PlayViewModel(
    private val statsEnabled: Boolean = false,
) : ViewModel() {
    var state: PlayState? by mutableStateOf(null)
        private set

    private var session: PlaySession? = null

    fun start(size: GridSize, random: Random) {
        if (session != null) return
        session = PlaySession(size, statsEnabled, random)
        publish()
    }

    fun onCell(cell: Cell) {
        session?.tap(cell)
        publish()
    }

    fun togglePeek() {
        session?.togglePeek()
        publish()
    }

    fun clearLastReverted() {
        session?.clearLastReverted()
        publish()
    }

    private fun publish() {
        val current = session ?: return
        state = PlayState(
            board = current.board,
            selected = current.selected,
            elapsedMs = 0,
            moves = current.moves,
            won = current.isWin(),
            peek = current.peek,
            lastReverted = current.lastReverted,
        )
    }
}
