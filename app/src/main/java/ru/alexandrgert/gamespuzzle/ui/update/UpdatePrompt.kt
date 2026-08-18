package ru.alexandrgert.gamespuzzle.ui.update

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import ru.alexandrgert.gamespuzzle.R
import ru.alexandrgert.gamespuzzle.data.UpdateCheckResult
import ru.alexandrgert.gamespuzzle.data.UpdateChecker

data class UpdateDialogState(
    val canConfirmDownload: Boolean,
    val showDownloadOverlay: Boolean,
)

fun buildUpdateDialogState(
    result: UpdateCheckResult,
    isDownloading: Boolean,
): UpdateDialogState {
    val canConfirmDownload = result.updateAvailable
    return UpdateDialogState(
        canConfirmDownload = canConfirmDownload,
        showDownloadOverlay = canConfirmDownload && isDownloading,
    )
}

@Composable
fun UpdateResultDialog(
    result: UpdateCheckResult,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onPostpone: () -> Unit,
) {
    val state = buildUpdateDialogState(result, isDownloading)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (result.updateAvailable) {
                        R.string.update_available
                    } else {
                        R.string.update_current
                    },
                ),
            )
        },
        text = { Text(result.changelog) },
        confirmButton = {
            if (state.canConfirmDownload) {
                Button(
                    onClick = onDownload,
                    enabled = !isDownloading,
                ) {
                    Text(stringResource(R.string.action_download))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        },
        dismissButton = {
            if (state.canConfirmDownload) {
                TextButton(onClick = onPostpone) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    )
    if (state.showDownloadOverlay) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.update_downloading))
                }
            },
        )
    }
}

suspend fun downloadUpdateApk(
    context: Context,
    client: OkHttpClient,
    url: String,
): File? = withContext(Dispatchers.IO) {
    val destination = File(context.cacheDir, "updates/games-puzzle.apk")
    if (UpdateChecker.download(client, url, destination)) destination else null
}
