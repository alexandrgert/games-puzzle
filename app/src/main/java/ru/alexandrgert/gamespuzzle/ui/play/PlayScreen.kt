package ru.alexandrgert.gamespuzzle.ui.play

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Random
import kotlin.math.min
import kotlinx.coroutines.delay
import ru.alexandrgert.gamespuzzle.R
import ru.alexandrgert.gamespuzzle.domain.Cell
import ru.alexandrgert.gamespuzzle.domain.GridSize

@Composable
fun PlayScreen(
    sourceBitmap: Bitmap?,
    size: GridSize,
    statsEnabled: Boolean,
    onAbandon: () -> Unit,
) {
    val playViewModel: PlayViewModel = viewModel {
        PlayViewModel(statsEnabled)
    }
    var confirmAbandon by remember { mutableStateOf(false) }
    var revertedFlash by remember { mutableStateOf(false) }
    val state = playViewModel.state

    LaunchedEffect(size, playViewModel) {
        if (playViewModel.state == null) {
            playViewModel.start(size, Random())
        }
    }
    LaunchedEffect(statsEnabled, playViewModel) {
        if (!statsEnabled) return@LaunchedEffect
        while (true) {
            delay(250)
            val current = playViewModel.state ?: continue
            if (current.won) break
            playViewModel.tick()
        }
    }
    LaunchedEffect(state?.lastReverted) {
        if (state?.lastReverted == true) {
            revertedFlash = true
            try {
                delay(180)
            } finally {
                revertedFlash = false
                playViewModel.clearLastReverted()
            }
        }
    }
    BackHandler {
        confirmAbandon = true
    }

    if (sourceBitmap == null || state == null) {
        Text(
            text = stringResource(R.string.play_image_not_found),
            modifier = Modifier.padding(16.dp),
        )
    } else {
        val squareBitmap = remember(sourceBitmap) {
            val squareSide = min(sourceBitmap.width, sourceBitmap.height)
            Bitmap.createScaledBitmap(
                sourceBitmap,
                squareSide,
                squareSide,
                true,
            )
        }
        val tiles = remember(squareBitmap, size) {
            List(size.n * size.n) { tileId ->
                tileBitmap(squareBitmap, tileId, size.n).asImageBitmap()
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                Column(Modifier.fillMaxSize()) {
                    repeat(size.n) { row ->
                        Row(Modifier.weight(1f)) {
                            repeat(size.n) { col ->
                                val cell = Cell(row, col)
                                val tileId = state.board.tileAt(cell)
                                val selectedModifier = if (state.selected == cell) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    Modifier
                                }
                                Image(
                                    bitmap = tiles[tileId],
                                    contentDescription = null,
                                    contentScale = ContentScale.FillBounds,
                                    modifier = selectedModifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clickable { playViewModel.onCell(cell) },
                                )
                            }
                        }
                    }
                }
                if (state.peek) {
                    Image(
                        bitmap = squareBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.puzzle_image),
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (revertedFlash) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.32f)),
                    )
                }
            }
            if (statsEnabled) {
                Text(
                    stringResource(
                        R.string.play_stats,
                        state.moves,
                        state.elapsedMs / 1_000,
                    ),
                )
            }
            Button(
                onClick = playViewModel::togglePeek,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_peek))
            }
        }

        if (state.won) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.win_title)) },
                text = { Text(stringResource(R.string.win_message)) },
                confirmButton = {
                    TextButton(onClick = onAbandon) {
                        Text(stringResource(R.string.action_ok))
                    }
                },
            )
        }
    }

    if (confirmAbandon) {
        AlertDialog(
            onDismissRequest = { confirmAbandon = false },
            title = { Text(stringResource(R.string.confirm_abandon_title)) },
            text = { Text(stringResource(R.string.confirm_abandon_message)) },
            confirmButton = {
                TextButton(onClick = onAbandon) {
                    Text(stringResource(R.string.action_abandon))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAbandon = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

fun tileBitmap(src: Bitmap, tileId: Int, n: Int): Bitmap {
    require(n > 0)
    require(tileId in 0 until n * n)
    val squareSide = min(src.width, src.height)
    val squareBitmap = Bitmap.createScaledBitmap(src, squareSide, squareSide, true)
    val tileSide = squareSide / n
    require(tileSide > 0)
    val homeRow = tileId / n
    val homeCol = tileId % n
    return Bitmap.createBitmap(
        squareBitmap,
        homeCol * tileSide,
        homeRow * tileSide,
        tileSide,
        tileSide,
    )
}
