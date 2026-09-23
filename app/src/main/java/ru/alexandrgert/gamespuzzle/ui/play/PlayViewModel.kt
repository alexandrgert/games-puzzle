package ru.alexandrgert.gamespuzzle.ui.play

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import java.util.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.alexandrgert.gamespuzzle.data.ActivePlaySessionStore
import ru.alexandrgert.gamespuzzle.data.RecordSaver
import ru.alexandrgert.gamespuzzle.data.RecordUpdate
import ru.alexandrgert.gamespuzzle.domain.Board
import ru.alexandrgert.gamespuzzle.domain.Cell
import ru.alexandrgert.gamespuzzle.domain.GridSize
import ru.alexandrgert.gamespuzzle.domain.PlaySession
import ru.alexandrgert.gamespuzzle.domain.PlaySessionSnapshot

data class PlayState(
    val board: Board,
    val selected: Cell?,
    val elapsedMs: Long,
    val moves: Int,
    val won: Boolean,
    val peek: Boolean,
    val recordSavePending: Boolean,
    val recordUpdate: RecordUpdate?,
)

class PlayViewModel(
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val statsEnabled: Boolean = false,
    private val puzzleId: String = "",
    private val recordSaver: RecordSaver? = null,
    private val activeSessionStore: ActivePlaySessionStore? = null,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    var state: PlayState? by mutableStateOf(null)
        private set

    private var session: PlaySession? = null
    private var persistenceJob: Job? = null
    private var starting = false
    private var foreground = true

    fun start(size: GridSize, random: Random) {
        if (session != null || starting) return
        val restored = restoredSession(size)
        if (restored != null || activeSessionStore == null) {
            session = restored ?: PlaySession(size, statsEnabled, random, currentTimeMillis)
            publish(saveWin = restored == null)
            return
        }
        starting = true
        viewModelScope.launch {
            val key = sessionKey(size)
            val persisted = try {
                activeSessionStore.load(key)
            } catch (cancelled: CancellationException) {
                starting = false
                throw cancelled
            } catch (_: Exception) {
                null
            }
            val durableSession = persisted
                ?.takeIf { it.sizeN == size.n && it.statsEnabled == statsEnabled }
                ?.let { PlaySession.restore(it, currentTimeMillis) }
            if (persisted != null && durableSession == null) activeSessionStore.clear(key)
            session = durableSession ?: PlaySession(size, statsEnabled, random, currentTimeMillis)
            if (!foreground) session?.pauseTimer()
            starting = false
            publish(saveWin = durableSession == null)
        }
    }

    fun onCell(cell: Cell) {
        session?.tap(cell)
        publish()
    }

    fun onSwap(a: Cell, b: Cell) {
        session?.swap(a, b)
        publish()
    }

    fun togglePeek() {
        session?.togglePeek()
        publish()
    }

    fun tick() {
        publish(persistDurably = false)
    }

    fun onBackground() {
        foreground = false
        session?.pauseTimer()
        publish()
    }

    fun onForeground() {
        foreground = true
        session?.resumeTimer()
        publish()
    }

    fun abandon() {
        val current = session ?: return
        val key = sessionKey(current.size)
        session = null
        state = null
        savedStateHandle.remove<PlaySessionSnapshot>(key)
        enqueuePersistence { activeSessionStore?.clear(key) }
    }

    private fun publish(saveWin: Boolean = true, persistDurably: Boolean = true) {
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
            recordSavePending = previous?.recordSavePending ?: false,
            recordUpdate = previous?.recordUpdate,
        )
        val key = sessionKey(current.size)
        val snapshot = current.snapshot()
        savedStateHandle[key] = snapshot
        if (persistDurably) {
            enqueuePersistence { activeSessionStore?.save(key, snapshot) }
        }
        if (saveWin && won && previous?.won != true) saveRecord(current)
    }

    private fun restoredSession(size: GridSize): PlaySession? {
        val snapshot = runCatching {
            savedStateHandle.get<PlaySessionSnapshot>(sessionKey(size))
        }.getOrNull() ?: return null
        if (snapshot.sizeN != size.n || snapshot.statsEnabled != statsEnabled) return null
        return PlaySession.restore(snapshot, currentTimeMillis)
    }

    private fun sessionKey(size: GridSize): String =
        "play-session:$puzzleId:${size.n}:$statsEnabled"

    private fun enqueuePersistence(block: suspend () -> Unit) {
        if (activeSessionStore == null) return
        val preceding = persistenceJob
        persistenceJob = viewModelScope.launch {
            preceding?.join()
            block()
        }
    }

    private fun saveRecord(current: PlaySession) {
        val saver = checkNotNull(recordSaver) {
            "A RecordSaver is required to save best results on win"
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
