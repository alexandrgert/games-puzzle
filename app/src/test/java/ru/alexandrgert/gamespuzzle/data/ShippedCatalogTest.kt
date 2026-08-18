package ru.alexandrgert.gamespuzzle.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.Category
import ru.alexandrgert.gamespuzzle.domain.Season

class ShippedCatalogTest {
    private val assets = File("src/main/assets")

    @Test
    fun shippedCatalogHasTwentyTwoHighReliefEntries() {
        val catalog = CatalogJson.parse(File(assets, "catalog.json").readText())
        assertEquals(22, catalog.puzzles.size)
        assertEquals(catalog.puzzles.map { it.id }.toSet().size, 22)
        assertEquals(Category.entries.toSet(), catalog.puzzles.map { it.category }.toSet())
        assertTrue(catalog.puzzles.map { it.season }.containsAll(Season.entries - Season.ANY))
        val oldIds = setOf(
            "altai-spring-nature", "kamchatka-geysers-summer", "moscow-autumn-forest",
            "baikal-winter-ice", "upornyj-amur-tiger", "novaya-zemlya-arctic-fox",
            "abakan-stellers-eagle", "kamchatka-harlequin-duck", "baikal-summer-shore",
            "olkhon-autumn-forest", "volga-lotus-summer", "moscow-lilac-spring",
            "olkhon-ice-cave-winter", "dzhangyskol-autumn-altai", "altai-birch-spring",
            "ladoga-pine-skerry", "autumn-asters", "kamchatka-brown-bear",
            "baikal-nerpa", "chistopol-bullfinch-winter", "krimozero-autumn",
            "lena-pillars-summer",
        )
        assertTrue(catalog.puzzles.none { it.id in oldIds })
        catalog.puzzles.forEach { puzzle ->
            assertTrue(File(assets, puzzle.file).isFile)
            assertTrue(File(assets, puzzle.thumb).isFile)
            assertTrue(puzzle.sourceUrl.startsWith("https://commons.wikimedia.org/wiki/File:"))
            assertTrue(!puzzle.sourceUrl.contains("File%3A"))
        }
    }
}
