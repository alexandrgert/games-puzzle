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
    fun winsOnAnyGridCountButInvalidRecordsDoNot() {
        assertEquals(setOf("a", "b"), completedPuzzleIds(mapOf(
            "rec:a:5" to "100,20", "rec:a:12" to "200,30",
            "rec:b:8" to "0,0", "rec:broken:6" to "bad",
            "rec:negative:10" to "-1,20", "rec:wrong-grid:9" to "100,20",
            "unrelated" to "100,20",
        )))
    }

    @Test
    fun malformedPreferenceValueIsIgnored() {
        assertNull(parseRecord("not-a-record"))
        assertNull(parseRecord("100,-2"))
        assertNull(parseRecord("-1,20"))
    }
}
