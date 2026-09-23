package ru.alexandrgert.gamespuzzle.domain

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaySessionTest {
    @Test
    fun startsWithShuffledPlayableBoard() {
        val session = PlaySession(GridSize.FIVE, statsEnabled = true, Random(1L))

        assertFalse(session.isWin())
        assertFalse(session.board.tiles.contentEquals(IntArray(25) { it }))
        assertTrue(BoardEngine.hasResultativeSwap(session.board))
    }

    @Test
    fun twoTapsApplySwapAndCountMoveWhenStatsEnabled() {
        val session = PlaySession(GridSize.FIVE, statsEnabled = true, Random(2L))
        val (first, second) = findPair(session.board, joined = true)

        assertNull(session.tap(first))
        val result = session.tap(second)

        assertTrue(result is MoveResult.Applied)
        assertNull(session.selected)
        assertEquals(1, session.moves)
    }

    @Test
    fun persistSwapCountsMoveAndKeepsTiles() {
        val session = PlaySession(GridSize.FIVE, statsEnabled = true, Random(3L))
        val (first, second) = findPair(session.board, joined = false)
        val firstTile = session.board.tileAt(first)
        val secondTile = session.board.tileAt(second)

        val result = session.swap(first, second) as MoveResult.Applied

        assertTrue(!result.joined)
        assertEquals(secondTile, session.board.tileAt(first))
        assertEquals(firstTile, session.board.tileAt(second))
        assertEquals(1, session.moves)
        assertNull(session.selected)
    }

    @Test
    fun dragSwapAppliesJoinWithoutPriorSelection() {
        val session = PlaySession(GridSize.FIVE, statsEnabled = true, Random(2L))
        val (first, second) = findPair(session.board, joined = true)

        val result = session.swap(first, second)

        assertTrue(result is MoveResult.Applied)
        assertNull(session.selected)
        assertEquals(1, session.moves)
    }

    @Test
    fun persistSwapCountsMoveWhenStatsDisabled() {
        val session = PlaySession(GridSize.FIVE, statsEnabled = false, Random(4L))
        val (first, second) = findPair(session.board, joined = false)
        assertTrue(session.swap(first, second) is MoveResult.Applied)
        assertEquals(1, session.moves)
    }

    @Test
    fun lockedSwapDoesNotCount() {
        val session = PlaySession(GridSize.FIVE, statsEnabled = true, Random(6L))
        val (first, second) = findPair(session.board, joined = true)
        session.swap(first, second)
        val locked = (0 until session.board.n * session.board.n)
            .map { Cell.fromIndex(it, session.board.n) }
            .first(session.board::isLockedCell)
        val unlocked = (0 until session.board.n * session.board.n)
            .map { Cell.fromIndex(it, session.board.n) }
            .first { !session.board.isLockedCell(it) }
        val movesBefore = session.moves
        val boardBefore = session.board
        assertTrue(session.swap(unlocked, locked) is MoveResult.Reverted)
        assertEquals(movesBefore, session.moves)
        assertSame(boardBefore, session.board)
    }

    @Test
    fun peekDoesNotCountAsMove() {
        val session = PlaySession(GridSize.FIVE, statsEnabled = true, Random(5L))

        session.togglePeek()

        assertTrue(session.peek)
        assertEquals(0, session.moves)
        session.togglePeek()
        assertFalse(session.peek)
        assertEquals(0, session.moves)
    }

    @Test
    fun tappingLockedCellIsIgnored() {
        val session = PlaySession(GridSize.FIVE, statsEnabled = true, Random(6L))
        val (first, second) = findPair(session.board, joined = true)
        session.tap(first)
        val applied = session.tap(second) as MoveResult.Applied
        val lockedCell = (0 until applied.board.n * applied.board.n)
            .map { Cell.fromIndex(it, applied.board.n) }
            .first(applied.board::isLockedCell)

        val selectedBefore = session.selected
        val movesBefore = session.moves
        assertNull(session.tap(lockedCell))

        assertEquals(selectedBefore, session.selected)
        assertEquals(movesBefore, session.moves)
        assertSame(applied.board, session.board)
    }

    @Test
    fun tappingSelectedCellClearsSelection() {
        val session = PlaySession(GridSize.FIVE, statsEnabled = true, Random(7L))
        val cell = Cell(0, 0)

        session.tap(cell)
        session.tap(cell)

        assertNull(session.selected)
        assertEquals(0, session.moves)
    }

    @Test
    fun snapshotRoundTripRestoresProgressAndContinuesTimer() {
        var now = 1_000L
        val session = PlaySession(GridSize.FIVE, true, Random(8L)) { now }
        val (first, second) = findPair(session.board, joined = false)
        session.swap(first, second)
        val selected = (0 until session.board.n * session.board.n)
            .map { Cell.fromIndex(it, session.board.n) }
            .first { !session.board.isLockedCell(it) }
        session.tap(selected)
        session.togglePeek()
        now = 1_600L

        val restored = PlaySession.restore(session.snapshot()) { now }!!

        assertTrue(session.board.tiles.contentEquals(restored.board.tiles))
        assertTrue(session.board.locked.contentEquals(restored.board.locked))
        assertEquals(selected, restored.selected)
        assertEquals(1, restored.moves)
        assertTrue(restored.peek)
        assertEquals(600L, restored.elapsedMs)
        now = 1_900L
        assertEquals(900L, restored.elapsedMs)
    }

    @Test
    fun invalidSnapshotIsRejected() {
        val session = PlaySession(GridSize.FIVE, true, Random(9L))
        val damaged = session.snapshot().copy(tiles = IntArray(25))

        assertNull(PlaySession.restore(damaged))
    }

    @Test
    fun completedSnapshotKeepsElapsedTimeFrozen() {
        var now = 2_000L
        val completed = PlaySessionSnapshot(
            sizeN = 5,
            statsEnabled = true,
            tiles = IntArray(25) { it },
            locked = BooleanArray(25) { true },
            selectedIndex = -1,
            moves = 7,
            peek = false,
            elapsedMs = 750L,
            completed = true,
        )

        val restored = PlaySession.restore(completed) { now }!!
        now = 9_000L

        assertTrue(restored.isWin())
        assertEquals(750L, restored.elapsedMs)
    }

    @Test
    fun pausedTimerExcludesBackgroundIntervalAfterResume() {
        var now = 1_000L
        val session = PlaySession(GridSize.FIVE, true, Random(18L)) { now }
        now = 1_600L

        session.pauseTimer()
        now = 8_000L
        assertEquals(600L, session.elapsedMs)

        session.resumeTimer()
        now = 8_250L
        assertEquals(850L, session.elapsedMs)
    }

    private fun findPair(board: Board, joined: Boolean): Pair<Cell, Cell> {
        val cells = (0 until board.n * board.n).map { Cell.fromIndex(it, board.n) }
        for (first in cells) {
            for (second in cells) {
                if (first == second) continue
                val result = BoardEngine.trySwap(board, first, second)
                if (result is MoveResult.Applied && result.joined == joined) {
                    return first to second
                }
            }
        }
        error("No matching pair")
    }
}
