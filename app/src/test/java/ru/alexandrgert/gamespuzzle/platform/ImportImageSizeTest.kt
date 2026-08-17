package ru.alexandrgert.gamespuzzle.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportImageSizeTest {
    @Test
    fun shortestSideBelowMinIsTooSmall() {
        assertNull(importSquareOutputSide(400))
    }

    @Test
    fun shortestSideBetweenMinAndCapIsKept() {
        assertEquals(800, importSquareOutputSide(800))
    }

    @Test
    fun shortestSideAboveCapIsCappedAtPlayBudget() {
        assertEquals(1200, importSquareOutputSide(4000))
    }
}
