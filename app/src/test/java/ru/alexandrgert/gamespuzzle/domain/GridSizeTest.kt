package ru.alexandrgert.gamespuzzle.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GridSizeTest {
    @Test
    fun supportedSizesIncludeEightTenTwelve() {
        assertEquals(listOf(5, 6, 8, 10, 12), GridSize.entries.map { it.n })
    }
}
