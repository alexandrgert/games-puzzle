package ru.alexandrgert.gamespuzzle.ui.catalog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.CatalogPuzzle
import ru.alexandrgert.gamespuzzle.domain.Category
import ru.alexandrgert.gamespuzzle.domain.Season

class NewPhotoBadgeTest {
    private val photo = CatalogPuzzle(
        id = "new-photo", file = "p.webp", thumb = "t.webp", category = Category.NATURE,
        season = Season.SUMMER, titleRu = "Фото", license = "CC0", attribution = "Автор",
        sourceUrl = "https://example.com/photo", isNew = true,
    )

    @Test
    fun onlyNewUnsolvedPhotosReceiveBadge() {
        assertTrue(shouldShowNew(photo, emptySet()))
        assertFalse(shouldShowNew(photo, setOf(photo.id)))
        assertFalse(shouldShowNew(photo.copy(isNew = false), emptySet()))
        assertTrue(shouldShowNew(photo, setOf("another-photo")))
    }

    @Test
    fun badgeDoesNotFlashBeforeSavedProgressLoads() {
        assertFalse(shouldShowNew(photo, null))
    }
}
