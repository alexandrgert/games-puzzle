package ru.alexandrgert.gamespuzzle.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordsTest {
    @Test
    fun mergeKeepsIndependentBests() {
        val a = mergeRecord(null, 5000, 40)
        val b = mergeRecord(a, 8000, 20)
        assertEquals(5000L, b.bestTimeMs)
        assertEquals(20, b.bestMoves)
    }
}
