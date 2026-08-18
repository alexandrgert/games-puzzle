package ru.alexandrgert.gamespuzzle.ui.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayTileChromeTest {
    @Test
    fun outlineOnlyUnlockedIdleTiles() {
        assertTrue(
            PlayTileChrome.shouldDrawUnlockedOutline(
                locked = false,
                peek = false,
                selected = false,
                dragging = false,
            ),
        )
        assertFalse(
            PlayTileChrome.shouldDrawUnlockedOutline(
                locked = true,
                peek = false,
                selected = false,
                dragging = false,
            ),
        )
        assertFalse(
            PlayTileChrome.shouldDrawUnlockedOutline(
                locked = false,
                peek = true,
                selected = false,
                dragging = false,
            ),
        )
        assertFalse(
            PlayTileChrome.shouldDrawUnlockedOutline(
                locked = false,
                peek = false,
                selected = true,
                dragging = false,
            ),
        )
        assertFalse(
            PlayTileChrome.shouldDrawUnlockedOutline(
                locked = false,
                peek = false,
                selected = false,
                dragging = true,
            ),
        )
    }
}
