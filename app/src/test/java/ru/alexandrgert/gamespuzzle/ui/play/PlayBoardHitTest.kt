package ru.alexandrgert.gamespuzzle.ui.play

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.Cell

class PlayBoardHitTest {
    @Test
    fun mapsOffsetToCellOnSquareBoard() {
        assertEquals(Cell(0, 0), cellAt(10f, 10f, n = 6, widthPx = 600f, heightPx = 600f))
        assertEquals(Cell(0, 5), cellAt(550f, 20f, n = 6, widthPx = 600f, heightPx = 600f))
        assertEquals(Cell(5, 0), cellAt(20f, 550f, n = 6, widthPx = 600f, heightPx = 600f))
        assertEquals(Cell(3, 2), cellAt(250f, 350f, n = 6, widthPx = 600f, heightPx = 600f))
    }

    @Test
    fun outOfBoundsIsNull() {
        assertNull(cellAt(-1f, 10f, n = 6, widthPx = 600f, heightPx = 600f))
        assertNull(cellAt(10f, 600f, n = 6, widthPx = 600f, heightPx = 600f))
        assertNull(cellAt(10f, 10f, n = 6, widthPx = 0f, heightPx = 600f))
    }
}
