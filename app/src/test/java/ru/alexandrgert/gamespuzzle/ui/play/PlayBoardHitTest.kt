package ru.alexandrgert.gamespuzzle.ui.play

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.Cell

class PlayBoardHitTest {
    @Test
    fun portraitBoardUsesItsOwnWidthAndHeightForTapsAndDrops() {
        assertEquals(Cell(2, 1), cellAt(150f, 450f, n = 3, widthPx = 300f, heightPx = 600f))
        assertEquals(Cell(2, 2), cellAt(299f, 599f, n = 3, widthPx = 300f, heightPx = 600f))
        assertNull(cellAt(350f, 450f, n = 3, widthPx = 300f, heightPx = 600f))
        assertNull(cellAt(-1f, 450f, n = 3, widthPx = 300f, heightPx = 600f))
    }

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
