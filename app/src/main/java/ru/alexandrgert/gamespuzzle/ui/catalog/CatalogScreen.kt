package ru.alexandrgert.gamespuzzle.ui.catalog

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.alexandrgert.gamespuzzle.R
import ru.alexandrgert.gamespuzzle.domain.CatalogFile
import ru.alexandrgert.gamespuzzle.domain.CatalogPuzzle
import ru.alexandrgert.gamespuzzle.domain.Category
import ru.alexandrgert.gamespuzzle.domain.Season

@Composable
fun CatalogScreen(
    catalog: CatalogFile,
    assets: AssetManager,
    onPuzzleClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onMyPhotosClick: () -> Unit,
    onCreditsClick: () -> Unit,
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedSeason by remember { mutableStateOf<Season?>(null) }
    val puzzles = filterCatalog(catalog.puzzles, selectedCategory, selectedSeason)

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilledTonalButton(onClick = onSettingsClick) {
                    Text(stringResource(R.string.action_settings))
                }
            }
            item {
                FilledTonalButton(onClick = onMyPhotosClick) {
                    Text(stringResource(R.string.action_my_photos))
                }
            }
            item {
                FilledTonalButton(onClick = onCreditsClick) {
                    Text(stringResource(R.string.action_credits))
                }
            }
        }
        FilterRow(
            values = listOf(null) + Category.entries,
            selected = selectedCategory,
            label = { it?.labelResource() ?: R.string.filter_all },
            onSelected = { selectedCategory = it },
        )
        FilterRow(
            values = listOf(null) + Season.entries.filterNot { it == Season.ANY },
            selected = selectedSeason,
            label = { it?.labelResource() ?: R.string.filter_all },
            onSelected = { selectedSeason = it },
        )
        if (puzzles.isEmpty()) {
            Text(
                text = stringResource(R.string.catalog_empty),
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(puzzles, key = { it.id }) { puzzle ->
                    CatalogThumbnail(
                        puzzle = puzzle,
                        assets = assets,
                        onClick = { onPuzzleClick(puzzle.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> FilterRow(
    values: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelected: (T) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(values) { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = { Text(stringResource(label(value))) },
            )
        }
    }
}

@Composable
private fun CatalogThumbnail(
    puzzle: CatalogPuzzle,
    assets: AssetManager,
    onClick: () -> Unit,
) {
    val bitmap = remember(puzzle.thumb, assets) {
        assets.open(puzzle.thumb).use(BitmapFactory::decodeStream)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.puzzle_image),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .size(160.dp),
        )
        Text(
            text = puzzle.titleRu,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

internal fun filterCatalog(
    puzzles: List<CatalogPuzzle>,
    category: Category?,
    season: Season?,
): List<CatalogPuzzle> = puzzles.filter { puzzle ->
    (category == null || puzzle.category == category) &&
        (season == null || puzzle.season == season || puzzle.season == Season.ANY)
}

@StringRes
private fun Category.labelResource(): Int = when (this) {
    Category.NATURE -> R.string.category_nature
    Category.ANIMALS -> R.string.category_animals
    Category.BIRDS -> R.string.category_birds
    Category.AQUATIC -> R.string.category_aquatic
    Category.TREES -> R.string.category_trees
    Category.FLOWERS -> R.string.category_flowers
}

@StringRes
private fun Season.labelResource(): Int = when (this) {
    Season.SPRING -> R.string.season_spring
    Season.SUMMER -> R.string.season_summer
    Season.AUTUMN -> R.string.season_autumn
    Season.WINTER -> R.string.season_winter
    Season.ANY -> R.string.season_any
}
