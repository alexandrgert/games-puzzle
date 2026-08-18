package ru.alexandrgert.gamespuzzle.ui.play

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
import ru.alexandrgert.gamespuzzle.domain.Board
import ru.alexandrgert.gamespuzzle.domain.BestRecord
import ru.alexandrgert.gamespuzzle.domain.Cell
import ru.alexandrgert.gamespuzzle.domain.GridSize
import ru.alexandrgert.gamespuzzle.domain.PlaySession

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
        val viewModel = PlayViewModel(
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
        val viewModel = PlayViewModel(
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

    private fun solve(viewModel: PlayViewModel) {
        val size = viewModel.state!!.board.size
        val tiles = IntArray(size.n * size.n) { it }
        tiles[0] = 1
        tiles[1] = 0
        val locked = BooleanArray(size.n * size.n) { true }
        locked[0] = false
        locked[1] = false
        val sessionField = PlayViewModel::class.java.getDeclaredField("session")
            .apply { isAccessible = true }
        val session = sessionField.get(viewModel) as PlaySession
        val boardField = PlaySession::class.java.getDeclaredField("board")
            .apply { isAccessible = true }
        boardField.set(session, Board(size, tiles, locked))

        viewModel.onCell(Cell(0, 0))
        viewModel.onCell(Cell(0, 1))
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
}
