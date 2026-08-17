package ru.alexandrgert.gamespuzzle.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.Category
import ru.alexandrgert.gamespuzzle.domain.Season

class CatalogJsonTest {
    @Test
    fun parsesCatalogFixture() {
        val text = checkNotNull(javaClass.getResource("/catalog_min.json")).readText()

        val catalog = CatalogJson.parse(text)

        assertEquals(1, catalog.schemaVersion)
        assertEquals((1..12).map { "ph-%02d".format(it) }, catalog.puzzles.map { it.id })
        assertEquals(Category.entries.toSet(), catalog.puzzles.map { it.category }.toSet())
        assertTrue(catalog.puzzles.map { it.season }.containsAll(Season.entries - Season.ANY))
        assertEquals("Весенняя природа", catalog.puzzles.first().titleRu)
        assertEquals("https://example.com/placeholders/ph-12", catalog.puzzles.last().sourceUrl)
    }

    @Test
    fun ignoresUnknownKeys() {
        val text = checkNotNull(javaClass.getResource("/catalog_min.json"))
            .readText()
            .replace("\"schema_version\": 1", "\"schema_version\": 1, \"future_field\": true")

        assertEquals(12, CatalogJson.parse(text).puzzles.size)
    }
}
