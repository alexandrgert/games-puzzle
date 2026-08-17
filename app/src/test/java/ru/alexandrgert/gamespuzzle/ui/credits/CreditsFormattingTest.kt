package ru.alexandrgert.gamespuzzle.ui.credits

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.CatalogPuzzle
import ru.alexandrgert.gamespuzzle.domain.Category
import ru.alexandrgert.gamespuzzle.domain.Season

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

    @Test
    fun launcherIconPuzzle_usesDzhangyskolCatalogEntry() {
        val icon = puzzle("dzhangyskol-autumn-altai")
        val other = puzzle("lena-pillars-summer")

        assertEquals(icon, launcherIconPuzzle(listOf(other, icon)))
        assertEquals(null, launcherIconPuzzle(listOf(other)))
    }

    private fun puzzle(id: String) = CatalogPuzzle(
        id = id,
        file = "puzzles/$id.webp",
        thumb = "thumbs/$id.webp",
        category = Category.NATURE,
        season = Season.AUTUMN,
        titleRu = id,
        license = "CC BY-SA 4.0",
        attribution = "Discoverynn",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:example.jpg",
    )
}
