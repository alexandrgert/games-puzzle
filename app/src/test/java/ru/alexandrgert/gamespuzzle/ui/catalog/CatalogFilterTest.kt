package ru.alexandrgert.gamespuzzle.ui.catalog

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.CatalogPuzzle
import ru.alexandrgert.gamespuzzle.domain.Category
import ru.alexandrgert.gamespuzzle.domain.Season

class CatalogFilterTest {
    private val springNature = puzzle("spring", Category.NATURE, Season.SPRING)
    private val anyNature = puzzle("any", Category.NATURE, Season.ANY)
    private val winterAnimals = puzzle("winter", Category.ANIMALS, Season.WINTER)

    @Test
    fun nullFiltersIncludeEveryPuzzle() {
        val puzzles = listOf(springNature, anyNature, winterAnimals)

        assertEquals(puzzles, filterCatalog(puzzles, category = null, season = null))
    }

    @Test
    fun anySeasonMatchesSelectedSeason() {
        val filtered = filterCatalog(
            listOf(springNature, anyNature, winterAnimals),
            category = null,
            season = Season.SPRING,
        )

        assertEquals(listOf(springNature, anyNature), filtered)
    }

    @Test
    fun categoryAndSeasonFiltersAreCombined() {
        val filtered = filterCatalog(
            listOf(springNature, anyNature, winterAnimals),
            category = Category.NATURE,
            season = Season.WINTER,
        )

        assertEquals(listOf(anyNature), filtered)
    }

    private fun puzzle(id: String, category: Category, season: Season) = CatalogPuzzle(
        id = id,
        file = "puzzles/$id.webp",
        thumb = "thumbs/$id.webp",
        category = category,
        season = season,
        titleRu = id,
        license = "",
        attribution = "",
        sourceUrl = "",
    )
}
