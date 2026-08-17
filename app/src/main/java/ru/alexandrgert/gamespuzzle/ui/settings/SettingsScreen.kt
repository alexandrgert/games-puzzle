package ru.alexandrgert.gamespuzzle.ui.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import ru.alexandrgert.gamespuzzle.R
import ru.alexandrgert.gamespuzzle.data.AppSettings
import ru.alexandrgert.gamespuzzle.data.SettingsStore
import ru.alexandrgert.gamespuzzle.data.UpdateCheckResult
import ru.alexandrgert.gamespuzzle.data.UpdateChecker
import ru.alexandrgert.gamespuzzle.data.UpdateDownloadMode
import ru.alexandrgert.gamespuzzle.domain.parseSemver
import ru.alexandrgert.gamespuzzle.platform.ApkInstaller

@Composable
fun SettingsScreen(
    settings: AppSettings,
    settingsStore: SettingsStore,
    versionName: String,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val client = remember { OkHttpClient() }
    val snackbarHostState = remember { SnackbarHostState() }
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    val offlineError = stringResource(R.string.error_offline)
    val downloadError = stringResource(R.string.error_download)
    val installPermissionError = stringResource(R.string.error_install_permission)

    val downloadAndInstall: suspend (UpdateCheckResult) -> Unit = download@{ result ->
        if (isDownloading) return@download
        isDownloading = true
        val apk = try {
            result.apkAssetUrl?.let { url ->
                downloadApk(context, client, url)
            }
        } finally {
            isDownloading = false
        }
        if (apk == null) {
            snackbarHostState.showSnackbar(downloadError)
        } else if (!ApkInstaller.install(context, apk)) {
            snackbarHostState.showSnackbar(installPermissionError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingSwitch(
                label = stringResource(R.string.settings_stats),
                checked = settings.statsEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { settingsStore.setStatsEnabled(enabled) }
                },
            )
            SettingSwitch(
                label = stringResource(R.string.settings_download_immediately),
                checked = settings.updateDownloadMode == UpdateDownloadMode.IMMEDIATE,
                onCheckedChange = { immediate ->
                    scope.launch {
                        settingsStore.setUpdateDownloadMode(
                            if (immediate) {
                                UpdateDownloadMode.IMMEDIATE
                            } else {
                                UpdateDownloadMode.CONFIRM
                            },
                        )
                    }
                },
            )
            Text(stringResource(R.string.settings_version, versionName))
            Button(
                onClick = {
                    scope.launch {
                        isChecking = true
                        updateResult = null
                        val result = withContext(Dispatchers.IO) {
                            UpdateChecker.check(client, parseSemver(versionName))
                        }
                        isChecking = false
                        if (result.ok) {
                            updateResult = result
                            if (result.updateAvailable &&
                                settings.updateDownloadMode == UpdateDownloadMode.IMMEDIATE
                            ) {
                                downloadAndInstall(result)
                            }
                        } else {
                            snackbarHostState.showSnackbar(offlineError)
                        }
                    }
                },
                enabled = !isChecking && !isDownloading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_check_update))
            }
        }
    }

    updateResult?.let { result ->
        val canConfirmDownload = result.updateAvailable &&
            settings.updateDownloadMode == UpdateDownloadMode.CONFIRM
        AlertDialog(
            onDismissRequest = { updateResult = null },
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
                        onClick = { scope.launch { downloadAndInstall(result) } },
                        enabled = !isDownloading,
                    ) {
                        Text(stringResource(R.string.action_download))
                    }
                } else {
                    TextButton(onClick = { updateResult = null }) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            },
            dismissButton = {
                if (canConfirmDownload) {
                    TextButton(onClick = { updateResult = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            },
        )
    }
}

private suspend fun downloadApk(
    context: Context,
    client: OkHttpClient,
    url: String,
): File? = withContext(Dispatchers.IO) {
    val destination = File(context.cacheDir, "updates/games-puzzle.apk")
    if (UpdateChecker.download(client, url, destination)) destination else null
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
