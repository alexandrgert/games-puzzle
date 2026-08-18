package ru.alexandrgert.gamespuzzle.ui.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.alexandrgert.gamespuzzle.R
import ru.alexandrgert.gamespuzzle.data.RecordUpdate

@Composable
fun WinDialog(
    elapsedMs: Long,
    moves: Int,
    recordUpdate: RecordUpdate?,
    navigationEnabled: Boolean,
    onAgain: () -> Unit,
    onCatalog: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.win_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.win_message))
                Text(stringResource(R.string.win_time, elapsedMs / 1_000))
                Text(stringResource(R.string.win_moves, moves))
                if (recordUpdate?.improvedTime == true) {
                    Text(stringResource(R.string.win_new_best_time))
                }
                if (recordUpdate?.improvedMoves == true) {
                    Text(stringResource(R.string.win_new_best_moves))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onAgain,
                enabled = navigationEnabled,
            ) {
                Text(stringResource(R.string.win_again))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCatalog,
                enabled = navigationEnabled,
            ) {
                Text(stringResource(R.string.win_catalog))
            }
        },
    )
}
