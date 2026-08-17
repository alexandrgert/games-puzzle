package ru.alexandrgert.gamespuzzle.ui.play

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.Random
import kotlinx.coroutines.launch
import ru.alexandrgert.gamespuzzle.data.RecordSaver
import ru.alexandrgert.gamespuzzle.data.RecordUpdate
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
    val recordSavePending: Boolean,
    val recordUpdate: RecordUpdate?,
)

class PlayViewModel(
    private val statsEnabled: Boolean = false,
    private val puzzleId: String = "",
    private val recordSaver: RecordSaver? = null,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    var state: PlayState? by mutableStateOf(null)
        private set

    private var session: PlaySession? = null

    fun start(size: GridSize, random: Random) {
        if (session != null) return
        session = PlaySession(size, statsEnabled, random, currentTimeMillis)
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

    fun tick() {
        publish()
    }

    private fun publish() {
        val current = session ?: return
        val previous = state
        val won = current.isWin()
        state = PlayState(
            board = current.board,
            selected = current.selected,
            elapsedMs = current.elapsedMs,
            moves = current.moves,
            won = won,
            peek = current.peek,
            lastReverted = current.lastReverted,
            recordSavePending = previous?.recordSavePending ?: false,
            recordUpdate = previous?.recordUpdate,
        )
        if (won && previous?.won != true && statsEnabled) saveRecord(current)
    }

    private fun saveRecord(current: PlaySession) {
        val saver = checkNotNull(recordSaver) {
            "A RecordSaver is required when statistics are enabled"
        }
        state = state?.copy(recordSavePending = true)
        viewModelScope.launch {
            try {
                val update = saver.save(
                    puzzleId = puzzleId,
                    n = current.size.n,
                    timeMs = current.elapsedMs,
                    moves = current.moves,
                )
                state = state?.copy(recordUpdate = update)
            } finally {
                state = state?.copy(recordSavePending = false)
            }
        }
    }
}
