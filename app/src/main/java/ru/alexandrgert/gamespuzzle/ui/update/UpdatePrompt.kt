package ru.alexandrgert.gamespuzzle.ui.update

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import ru.alexandrgert.gamespuzzle.R
import ru.alexandrgert.gamespuzzle.data.UpdateCheckResult
import ru.alexandrgert.gamespuzzle.data.UpdateChecker
import ru.alexandrgert.gamespuzzle.data.UpdateDownloadMode

@Composable
fun UpdateResultDialog(
    result: UpdateCheckResult,
    downloadMode: UpdateDownloadMode,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onPostpone: () -> Unit,
) {
    val canConfirmDownload = result.updateAvailable &&
        downloadMode == UpdateDownloadMode.CONFIRM
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
            if (canConfirmDownload) {
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
            if (canConfirmDownload) {
                TextButton(onClick = onPostpone) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    )
}

suspend fun downloadUpdateApk(
    context: Context,
    client: OkHttpClient,
    url: String,
): File? = withContext(Dispatchers.IO) {
    val destination = File(context.cacheDir, "updates/games-puzzle.apk")
    if (UpdateChecker.download(client, url, destination)) destination else null
}
