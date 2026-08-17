package ru.alexandrgert.gamespuzzle.ui.preview

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewScreen(
    puzzle: CatalogPuzzle?,
    assets: AssetManager,
    userBitmap: Bitmap? = null,
    onStart: (GridSize) -> Unit,
) {
    var gridSize by remember { mutableStateOf(GridSize.FIVE) }
    val bitmap = remember(puzzle?.file, assets, userBitmap) {
        userBitmap ?: puzzle?.let {
            runCatching {
                assets.open(it.file).use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
    }

    if (bitmap == null) {
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
        Text(puzzle?.titleRu ?: stringResource(R.string.user_photo_title))
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.puzzle_image),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GridSize.entries.forEach { sizeOption ->
                FilterChip(
                    selected = gridSize == sizeOption,
                    onClick = { gridSize = sizeOption },
                    label = { Text(stringResource(R.string.grid_size, sizeOption.n)) },
                )
            }
        }
        Button(
            onClick = { onStart(gridSize) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_start))
        }
    }
}
