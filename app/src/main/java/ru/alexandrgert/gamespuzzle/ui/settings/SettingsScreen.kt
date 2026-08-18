package ru.alexandrgert.gamespuzzle.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.alexandrgert.gamespuzzle.R
import ru.alexandrgert.gamespuzzle.data.AppSettings
import ru.alexandrgert.gamespuzzle.data.SettingsStore

@Composable
fun SettingsScreen(
    settings: AppSettings,
    settingsStore: SettingsStore,
    versionName: String,
    isCheckingUpdate: Boolean,
    isDownloadingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    Scaffold { innerPadding ->
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
                label = stringResource(R.string.settings_check_updates_on_launch),
                checked = settings.autoCheckUpdates,
                onCheckedChange = { enabled ->
                    scope.launch { settingsStore.setAutoCheckUpdates(enabled) }
                },
            )
            Text(stringResource(R.string.settings_version, versionName))
            Button(
                onClick = onCheckUpdate,
                enabled = !isCheckingUpdate && !isDownloadingUpdate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_check_update))
            }
        }
    }
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
