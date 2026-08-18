package ru.alexandrgert.gamespuzzle.ui.play

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Random
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import ru.alexandrgert.gamespuzzle.R
import ru.alexandrgert.gamespuzzle.data.RecordsStore
import ru.alexandrgert.gamespuzzle.domain.Cell
import ru.alexandrgert.gamespuzzle.domain.GridSize

@Composable
fun PlayScreen(
    puzzleId: String,
    sourceBitmap: Bitmap?,
    size: GridSize,
    statsEnabled: Boolean,
    recordsStore: RecordsStore,
    onAbandon: () -> Unit,
    onAgain: () -> Unit,
    onCatalog: () -> Unit,
) {
    val playViewModel: PlayViewModel = viewModel(key = "$puzzleId:${size.n}:$statsEnabled") {
        PlayViewModel(
            statsEnabled = statsEnabled,
            puzzleId = puzzleId,
            recordSaver = recordsStore,
        )
    }
    var confirmAbandon by remember { mutableStateOf(false) }
    val state = playViewModel.state

    LaunchedEffect(size, playViewModel) {
        if (playViewModel.state == null) {
            playViewModel.start(size, Random())
        }
    }
    LaunchedEffect(playViewModel) {
        while (true) {
            delay(250)
            val current = playViewModel.state ?: continue
            if (current.won) break
            playViewModel.tick()
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
        var boardPx by remember { mutableStateOf(IntSize.Zero) }
        var dragging by remember { mutableStateOf<Cell?>(null) }
        var dragStart by remember { mutableStateOf(Offset.Zero) }
        var dragDelta by remember { mutableStateOf(Offset.Zero) }
        val density = LocalDensity.current
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { boardPx = it }
                    .pointerInput(size.n, boardPx, state.peek, state.won) {
                        if (state.peek || state.won) return@pointerInput
                        detectTapGestures { offset ->
                            cellAt(
                                offset.x,
                                offset.y,
                                size.n,
                                boardPx.width.toFloat(),
                                boardPx.height.toFloat(),
                            )?.let(playViewModel::onCell)
                        }
                    }
                    .pointerInput(size.n, boardPx, state.peek, state.won, state.board) {
                        if (state.peek || state.won) return@pointerInput
                        detectDragGestures(
                            onDragStart = { offset ->
                                val cell = cellAt(
                                    offset.x,
                                    offset.y,
                                    size.n,
                                    boardPx.width.toFloat(),
                                    boardPx.height.toFloat(),
                                )
                                if (cell != null && !state.board.isLockedCell(cell)) {
                                    dragging = cell
                                    dragStart = offset
                                    dragDelta = Offset.Zero
                                } else {
                                    dragging = null
                                }
                            },
                            onDrag = { _, amount ->
                                if (dragging != null) dragDelta += amount
                            },
                            onDragEnd = {
                                val from = dragging
                                val drop = cellAt(
                                    dragStart.x + dragDelta.x,
                                    dragStart.y + dragDelta.y,
                                    size.n,
                                    boardPx.width.toFloat(),
                                    boardPx.height.toFloat(),
                                )
                                dragging = null
                                dragDelta = Offset.Zero
                                if (from != null && drop != null && drop != from) {
                                    playViewModel.onSwap(from, drop)
                                }
                            },
                            onDragCancel = {
                                dragging = null
                                dragDelta = Offset.Zero
                            },
                        )
                    },
            ) {
                Column(Modifier.fillMaxSize()) {
                    repeat(size.n) { row ->
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            repeat(size.n) { col ->
                                val cell = Cell(row, col)
                                val tileId = tileShownAt(state, cell)
                                val selectedModifier = if (state.selected == cell) {
                                    Modifier.border(4.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    Modifier
                                }
                                Box(
                                    modifier = selectedModifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                ) {
                                    if (dragging == cell) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                ),
                                        )
                                    } else {
                                        Image(
                                            bitmap = tiles[tileId],
                                            contentDescription = null,
                                            contentScale = ContentScale.FillBounds,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                val dragCell = dragging
                if (dragCell != null && boardPx.width > 0 && boardPx.height > 0) {
                    val cellW = boardPx.width / size.n.toFloat()
                    val cellH = boardPx.height / size.n.toFloat()
                    val originX = dragCell.col * cellW
                    val originY = dragCell.row * cellH
                    Image(
                        bitmap = tiles[state.board.tileAt(dragCell)],
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .zIndex(1f)
                            .offset {
                                IntOffset(
                                    (originX + dragDelta.x).roundToInt(),
                                    (originY + dragDelta.y).roundToInt(),
                                )
                            }
                            .size(
                                with(density) { cellW.toDp() },
                                with(density) { cellH.toDp() },
                            )
                            .border(4.dp, MaterialTheme.colorScheme.primary),
                    )
                }
                if (state.peek) {
                    Image(
                        bitmap = squareBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.puzzle_image),
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { playViewModel.togglePeek() }
                            },
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            Button(
                onClick = playViewModel::togglePeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Text(stringResource(R.string.action_peek))
            }
        }

        if (state.won) {
            WinDialog(
                elapsedMs = state.elapsedMs,
                moves = state.moves,
                recordUpdate = state.recordUpdate,
                navigationEnabled = !state.recordSavePending,
                onAgain = onAgain,
                onCatalog = onCatalog,
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

private fun tileShownAt(state: PlayState, cell: Cell): Int = state.board.tileAt(cell)

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
