package ru.alexandrgert.gamespuzzle.data

import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Test

class AssetCatalogTest {
    @Test
    fun dropsPuzzlesWithMissingImageOrThumbnail() {
        val catalog = checkNotNull(javaClass.getResource("/catalog_min.json")).readText()
        val missing = setOf("puzzles/ph-02.webp", "thumbs/ph-03.webp")

        val loaded = AssetCatalog.load { path ->
            if (path == "catalog.json") {
                ByteArrayInputStream(catalog.toByteArray())
            } else if (path in missing) {
                throw FileNotFoundException(path)
            } else {
                ByteArrayInputStream(byteArrayOf(1))
            }
        }

        assertEquals(
            listOf("ph-01") + (4..12).map { "ph-%02d".format(it) },
            loaded.puzzles.map { it.id },
        )
    }

    @Test
    fun returnsEmptyCatalogWhenCatalogCannotBeRead() {
        val loaded = AssetCatalog.load { throw FileNotFoundException(it) }

        assertEquals(emptyList<String>(), loaded.puzzles.map { it.id })
    }
}
