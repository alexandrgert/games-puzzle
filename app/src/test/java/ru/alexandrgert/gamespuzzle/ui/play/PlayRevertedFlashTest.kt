package ru.alexandrgert.gamespuzzle.ui.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.Cell

class PlayRevertedFlashTest {
    private val a = Cell(1, 2)
    private val b = Cell(3, 0)
    private val other = Cell(0, 0)

    @Test
    fun onlyAttemptedPairFlashesWhileReverted() {
        assertTrue(isRevertedFlashCell(a, lastReverted = true, revertedA = a, revertedB = b))
        assertTrue(isRevertedFlashCell(b, lastReverted = true, revertedA = a, revertedB = b))
        assertFalse(isRevertedFlashCell(other, lastReverted = true, revertedA = a, revertedB = b))
    }

    @Test
    fun noCellFlashesAfterRevertClears() {
        assertFalse(isRevertedFlashCell(a, lastReverted = false, revertedA = a, revertedB = b))
        assertFalse(isRevertedFlashCell(b, lastReverted = false, revertedA = a, revertedB = b))
    }
}
