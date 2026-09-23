package ru.alexandrgert.gamespuzzle.ui.play

import androidx.lifecycle.SavedStateHandle
import java.util.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.alexandrgert.gamespuzzle.data.RecordSaver
import ru.alexandrgert.gamespuzzle.data.RecordUpdate
import ru.alexandrgert.gamespuzzle.data.ActivePlaySessionStore
import ru.alexandrgert.gamespuzzle.domain.BoardEngine
import ru.alexandrgert.gamespuzzle.domain.BestRecord
import ru.alexandrgert.gamespuzzle.domain.Cell
import ru.alexandrgert.gamespuzzle.domain.GridSize
import ru.alexandrgert.gamespuzzle.domain.PlaySession
import ru.alexandrgert.gamespuzzle.domain.PlaySessionSnapshot

@OptIn(ExperimentalCoroutinesApi::class)
class PlayViewModelTest {
    @Before
    fun setUpMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun elapsedTimeIncreasesAfterStart() {
        var now = 1_000L
        val viewModel = PlayViewModel(currentTimeMillis = { now })
        viewModel.start(GridSize.FIVE, Random(1L))

        now = 1_450L
        viewModel.togglePeek()

        assertEquals(450L, viewModel.state!!.elapsedMs)
    }

    @Test
    fun elapsedTimeUpdatesOnTickWhileIdle() {
        var now = 1_000L
        val viewModel = PlayViewModel(statsEnabled = true, currentTimeMillis = { now })
        viewModel.start(GridSize.FIVE, Random(1L))

        now = 1_500L
        viewModel.tick()

        assertEquals(500L, viewModel.state!!.elapsedMs)
    }

    @Test
    fun repeatedStartKeepsTheActiveBoard() {
        val viewModel = PlayViewModel()
        viewModel.start(GridSize.FIVE, Random(1L))
        val originalBoard = viewModel.state!!.board

        viewModel.start(GridSize.FIVE, Random(2L))

        assertSame(originalBoard, viewModel.state!!.board)
    }

    @Test
    fun winningPersistsRecordInViewModelAndPublishesMergeResult() {
        val releaseSave = CompletableDeferred<Unit>()
        val update = RecordUpdate(
            record = BestRecord(bestTimeMs = 450L, bestMoves = 5),
            improvedTime = true,
            improvedMoves = false,
        )
        val saver = FakeRecordSaver(releaseSave, update)
        var now = 1_000L
        val handle = nearWinHandle("puzzle-1", statsEnabled = true)
        val viewModel = PlayViewModel(
            savedStateHandle = handle,
            statsEnabled = true,
            puzzleId = "puzzle-1",
            recordSaver = saver,
            currentTimeMillis = { now },
        )
        viewModel.start(GridSize.FIVE, Random(4L))

        now = 1_450L
        solve(viewModel)

        assertTrue(viewModel.state!!.won)
        assertTrue(viewModel.state!!.recordSavePending)
        assertNull(viewModel.state!!.recordUpdate)
        assertEquals("puzzle-1", saver.puzzleId)
        assertEquals(GridSize.FIVE.n, saver.n)

        releaseSave.complete(Unit)

        assertFalse(viewModel.state!!.recordSavePending)
        assertSame(update, viewModel.state!!.recordUpdate)
    }

    @Test
    fun winningWithStatsDisabledSavesRecord() {
        val releaseSave = CompletableDeferred<Unit>()
        val update = RecordUpdate(
            record = BestRecord(bestTimeMs = 400L, bestMoves = 3),
            improvedTime = true,
            improvedMoves = true,
        )
        val saver = FakeRecordSaver(releaseSave, update)
        val handle = nearWinHandle("puzzle-2", statsEnabled = false)
        val viewModel = PlayViewModel(
            savedStateHandle = handle,
            statsEnabled = false,
            puzzleId = "puzzle-2",
            recordSaver = saver,
        )
        viewModel.start(GridSize.FIVE, Random(5L))
        solve(viewModel)
        assertTrue(viewModel.state!!.won)
        assertTrue(viewModel.state!!.recordSavePending)
        assertEquals(1, saver.calls)
        releaseSave.complete(Unit)
        assertFalse(viewModel.state!!.recordSavePending)
        assertSame(update, viewModel.state!!.recordUpdate)
    }

    @Test
    fun elapsedTimeTicksWhenStatsDisabled() {
        var now = 1_000L
        val viewModel = PlayViewModel(statsEnabled = false, currentTimeMillis = { now })
        viewModel.start(GridSize.FIVE, Random(1L))
        now = 1_500L
        viewModel.tick()
        assertEquals(500L, viewModel.state!!.elapsedMs)
    }

    @Test
    fun savedStateRestoresTheSameSessionAfterRecreation() {
        val handle = SavedStateHandle()
        var now = 1_000L
        val original = PlayViewModel(
            savedStateHandle = handle,
            puzzleId = "puzzle-restore",
            currentTimeMillis = { now },
        )
        original.start(GridSize.FIVE, Random(10L))
        original.togglePeek()
        original.onCell(Cell(0, 0))
        now = 1_500L
        original.tick()

        val restored = PlayViewModel(
            savedStateHandle = handle,
            puzzleId = "puzzle-restore",
            currentTimeMillis = { now },
        )
        restored.start(GridSize.FIVE, Random(99L))

        assertTrue(original.state!!.board.tiles.contentEquals(restored.state!!.board.tiles))
        assertEquals(original.state!!.selected, restored.state!!.selected)
        assertTrue(restored.state!!.peek)
        assertEquals(500L, restored.state!!.elapsedMs)
    }

    @Test
    fun savedStateFromAnotherPuzzleIsIgnored() {
        val handle = SavedStateHandle()
        val first = PlayViewModel(savedStateHandle = handle, puzzleId = "first")
        first.start(GridSize.FIVE, Random(11L))

        val expected = PlaySession(GridSize.FIVE, false, Random(12L)).board.tiles
        val second = PlayViewModel(savedStateHandle = handle, puzzleId = "second")
        second.start(GridSize.FIVE, Random(12L))

        assertTrue(expected.contentEquals(second.state!!.board.tiles))
    }

    @Test
    fun savedStateWithIncompatibleSizeIsIgnored() {
        val snapshot = PlaySession(GridSize.SIX, false, Random(14L)).snapshot()
        val handle = SavedStateHandle(
            mapOf("play-session:puzzle:5:false" to snapshot),
        )
        val expected = PlaySession(GridSize.FIVE, false, Random(15L)).board.tiles
        val viewModel = PlayViewModel(savedStateHandle = handle, puzzleId = "puzzle")

        viewModel.start(GridSize.FIVE, Random(15L))

        assertTrue(expected.contentEquals(viewModel.state!!.board.tiles))
    }

    @Test
    fun savedStateWithIncompatibleStatsSettingIsIgnored() {
        val snapshot = PlaySession(GridSize.FIVE, true, Random(16L)).snapshot()
        val handle = SavedStateHandle(
            mapOf("play-session:puzzle:5:false" to snapshot),
        )
        val expected = PlaySession(GridSize.FIVE, false, Random(17L)).board.tiles
        val viewModel = PlayViewModel(savedStateHandle = handle, puzzleId = "puzzle")

        viewModel.start(GridSize.FIVE, Random(17L))

        assertTrue(expected.contentEquals(viewModel.state!!.board.tiles))
    }

    @Test
    fun damagedSavedStateIsIgnored() {
        val handle = SavedStateHandle(
            mapOf(
                "play-session:puzzle:5:false" to PlaySessionSnapshot(
                    sizeN = 5,
                    statsEnabled = false,
                    tiles = IntArray(25),
                    locked = BooleanArray(25),
                    selectedIndex = -1,
                    moves = 0,
                    peek = false,
                    elapsedMs = 0L,
                    completed = false,
                ),
            ),
        )
        val viewModel = PlayViewModel(savedStateHandle = handle, puzzleId = "puzzle")

        viewModel.start(GridSize.FIVE, Random(13L))

        assertTrue(BoardEngine.hasResultativeSwap(viewModel.state!!.board))
    }

    @Test
    fun durableStoreRestoresSessionWithoutSavedStateHandle() {
        val durableData = mutableMapOf<String, PlaySessionSnapshot>()
        val firstStore = FakeActivePlaySessionStore(durableData)
        var now = 1_000L
        val original = PlayViewModel(
            puzzleId = "durable",
            activeSessionStore = firstStore,
            currentTimeMillis = { now },
        )
        original.start(GridSize.FIVE, Random(21L))
        val board = original.state!!.board
        val cells = (0 until 25).map { Cell.fromIndex(it, 5) }
        val joinedPair = cells.asSequence().flatMap { first ->
            cells.asSequence().map { second -> first to second }
        }.first { (first, second) ->
            first != second && (BoardEngine.trySwap(board, first, second) as? ru.alexandrgert.gamespuzzle.domain.MoveResult.Applied)?.joined == true
        }
        original.onSwap(joinedPair.first, joinedPair.second)
        original.togglePeek()
        val selected = cells.first { !original.state!!.board.isLockedCell(it) }
        original.onCell(selected)
        now = 1_400L
        original.tick()
        original.onBackground()

        val restored = PlayViewModel(
            puzzleId = "durable",
            activeSessionStore = FakeActivePlaySessionStore(durableData),
            currentTimeMillis = { now },
        )
        restored.start(GridSize.FIVE, Random(22L))

        assertTrue(original.state!!.board.tiles.contentEquals(restored.state!!.board.tiles))
        assertTrue(original.state!!.board.locked.contentEquals(restored.state!!.board.locked))
        assertEquals(selected, restored.state!!.selected)
        assertEquals(1, restored.state!!.moves)
        assertTrue(restored.state!!.peek)
        assertEquals(400L, restored.state!!.elapsedMs)
    }

    @Test
    fun completedDurableSessionKeepsFinalTime() {
        val key = "play-session:complete:5:false"
        val store = FakeActivePlaySessionStore(
            mutableMapOf(
                key to PlaySessionSnapshot(
                    5, false, IntArray(25) { it }, BooleanArray(25) { true },
                    -1, 9, false, 725L, true,
                ),
            ),
        )
        var now = 5_000L
        val viewModel = PlayViewModel(
            puzzleId = "complete",
            activeSessionStore = store,
            currentTimeMillis = { now },
        )
        viewModel.start(GridSize.FIVE, Random(26L))
        now = 20_000L
        viewModel.tick()

        assertTrue(viewModel.state!!.won)
        assertEquals(725L, viewModel.state!!.elapsedMs)
        assertEquals(9, viewModel.state!!.moves)
    }

    @Test
    fun invalidDurableSnapshotIsClearedAndReplaced() {
        val store = FakeActivePlaySessionStore().apply {
            snapshots["play-session:damaged:5:false"] = PlaySessionSnapshot(
                sizeN = 5,
                statsEnabled = false,
                tiles = IntArray(25),
                locked = BooleanArray(25),
                selectedIndex = -1,
                moves = 0,
                peek = false,
                elapsedMs = 0,
                completed = false,
            )
        }
        val viewModel = PlayViewModel(puzzleId = "damaged", activeSessionStore = store)

        viewModel.start(GridSize.FIVE, Random(23L))

        assertEquals(1, store.clears)
        assertTrue(BoardEngine.hasResultativeSwap(viewModel.state!!.board))
    }

    @Test
    fun backgroundTimeIsNotAddedAfterForeground() {
        var now = 1_000L
        val viewModel = PlayViewModel(currentTimeMillis = { now })
        viewModel.start(GridSize.FIVE, Random(24L))
        now = 1_500L
        viewModel.onBackground()
        now = 9_000L
        viewModel.onForeground()
        now = 9_300L
        viewModel.tick()

        assertEquals(800L, viewModel.state!!.elapsedMs)
    }

    @Test
    fun persistentWritesKeepMutationOrder() {
        val firstWriteMayFinish = CompletableDeferred<Unit>()
        val store = FakeActivePlaySessionStore(saveGate = firstWriteMayFinish)
        val viewModel = PlayViewModel(puzzleId = "ordered", activeSessionStore = store)
        viewModel.start(GridSize.FIVE, Random(25L))
        viewModel.togglePeek()
        viewModel.onCell(Cell(0, 0))

        assertTrue(store.saved.isEmpty())
        assertEquals(1, store.saveCallsStarted)

        firstWriteMayFinish.complete(Unit)

        assertEquals(3, store.saveCallsStarted)
        assertEquals(3, store.saved.size)
        assertFalse(store.saved[0].peek)
        assertTrue(store.saved[1].peek)
        assertEquals(-1, store.saved[1].selectedIndex)
        assertEquals(0, store.saved[2].selectedIndex)
    }

    @Test
    fun readFailureDoesNotLeaveStartStuck() {
        val store = FakeActivePlaySessionStore(loadFailure = IllegalStateException("read failed"))
        val viewModel = PlayViewModel(puzzleId = "read-failure", activeSessionStore = store)

        viewModel.start(GridSize.FIVE, Random(28L))

        assertEquals(1, store.loads)
        assertTrue(BoardEngine.hasResultativeSwap(viewModel.state!!.board))
        assertEquals(1, store.saved.size)
    }

    @Test
    fun tickUpdatesUiWithoutWritingUntilBackground() {
        val store = FakeActivePlaySessionStore()
        var now = 1_000L
        val viewModel = PlayViewModel(
            puzzleId = "tick",
            activeSessionStore = store,
            currentTimeMillis = { now },
        )
        viewModel.start(GridSize.FIVE, Random(27L))
        assertEquals(1, store.saved.size)

        now = 1_500L
        viewModel.tick()

        assertEquals(500L, viewModel.state!!.elapsedMs)
        assertEquals(1, store.saved.size)

        viewModel.onBackground()

        assertEquals(2, store.saved.size)
        assertEquals(500L, store.saved.last().elapsedMs)
    }

    private fun solve(viewModel: PlayViewModel) {
        viewModel.onCell(Cell(0, 0))
        viewModel.onCell(Cell(0, 1))
    }

    private fun nearWinHandle(puzzleId: String, statsEnabled: Boolean): SavedStateHandle {
        val tiles = IntArray(25) { it }.apply {
            this[0] = 1
            this[1] = 0
        }
        val locked = BooleanArray(25) { true }.apply {
            this[0] = false
            this[1] = false
        }
        return SavedStateHandle(
            mapOf(
                "play-session:$puzzleId:5:$statsEnabled" to PlaySessionSnapshot(
                    sizeN = 5,
                    statsEnabled = statsEnabled,
                    tiles = tiles,
                    locked = locked,
                    selectedIndex = -1,
                    moves = 0,
                    peek = false,
                    elapsedMs = 0L,
                    completed = false,
                ),
            ),
        )
    }

    private class FakeRecordSaver(
        private val releaseSave: CompletableDeferred<Unit>,
        private val update: RecordUpdate,
    ) : RecordSaver {
        var calls = 0
        var puzzleId: String? = null
        var n: Int? = null

        override suspend fun save(
            puzzleId: String,
            n: Int,
            timeMs: Long,
            moves: Int,
        ): RecordUpdate {
            calls++
            this.puzzleId = puzzleId
            this.n = n
            releaseSave.await()
            return update
        }
    }

    private class FakeActivePlaySessionStore(
        val snapshots: MutableMap<String, PlaySessionSnapshot> = mutableMapOf(),
        private val saveGate: CompletableDeferred<Unit>? = null,
        private val loadFailure: Exception? = null,
    ) : ActivePlaySessionStore {
        var clears = 0
        var loads = 0
        var saveCallsStarted = 0
        val saved = mutableListOf<PlaySessionSnapshot>()

        override suspend fun load(key: String): PlaySessionSnapshot? {
            loads++
            loadFailure?.let { throw it }
            return snapshots[key]
        }

        override suspend fun save(key: String, snapshot: PlaySessionSnapshot) {
            saveCallsStarted++
            if (saveCallsStarted == 1) saveGate?.await()
            saved += snapshot
            snapshots[key] = snapshot
        }

        override suspend fun clear(key: String) {
            clears++
            snapshots.remove(key)
        }
    }
}
