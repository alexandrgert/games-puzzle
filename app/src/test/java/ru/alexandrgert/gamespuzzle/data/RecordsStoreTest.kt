package ru.alexandrgert.gamespuzzle.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.BestRecord

class RecordsStoreTest {
    @Test
    fun recordRoundTripsThroughPreferenceValue() {
        val record = BestRecord(bestTimeMs = 12_345L, bestMoves = 67)

        assertEquals(record, parseRecord(serializeRecord(record)))
    }

    @Test
    fun malformedPreferenceValueIsIgnored() {
        assertNull(parseRecord("not-a-record"))
        assertNull(parseRecord("100,-2"))
        assertNull(parseRecord("-1,20"))
    }
}
