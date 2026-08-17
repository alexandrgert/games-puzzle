package ru.alexandrgert.gamespuzzle.ui.credits

import org.junit.Assert.assertEquals
import org.junit.Test

class CreditsFormattingTest {
    @Test
    fun formatCreditsAttribution_joinsWithEmDash() {
        assertEquals(
            "Весенняя природа — Generated placeholder — CC0-1.0",
            formatCreditsAttribution(
                titleRu = "Весенняя природа",
                attribution = "Generated placeholder",
                license = "CC0-1.0",
            ),
        )
    }
}
