package ru.alexandrgert.gamespuzzle.ui.preview

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import ru.alexandrgert.gamespuzzle.domain.CatalogPuzzle
import ru.alexandrgert.gamespuzzle.domain.GridSize

@Composable
fun PreviewScreen(
    puzzle: CatalogPuzzle?,
    assets: AssetManager,
    onStart: (GridSize) -> Unit,
) {
    var gridSize by remember { mutableStateOf(GridSize.FIVE) }
    val bitmap = remember(puzzle?.file, assets) {
        puzzle?.let {
            runCatching {
                assets.open(it.file).use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
    }

    if (puzzle == null || bitmap == null) {
        Text(
            text = stringResource(R.string.preview_not_found),
            modifier = Modifier.padding(16.dp),
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(puzzle.titleRu)
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.puzzle_image),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilterChip(
                selected = gridSize == GridSize.FIVE,
                onClick = { gridSize = GridSize.FIVE },
                label = { Text(stringResource(R.string.grid_5)) },
            )
            FilterChip(
                selected = gridSize == GridSize.SIX,
                onClick = { gridSize = GridSize.SIX },
                label = { Text(stringResource(R.string.grid_6)) },
            )
        }
        Button(
            onClick = { onStart(gridSize) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_start))
        }
    }
}
