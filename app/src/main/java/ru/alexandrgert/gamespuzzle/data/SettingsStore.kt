package ru.alexandrgert.gamespuzzle.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class UpdateDownloadMode(val storedValue: String) {
    CONFIRM("confirm"),
    IMMEDIATE("immediate");

    companion object {
        fun fromStoredValue(value: String?): UpdateDownloadMode =
            entries.firstOrNull { it.storedValue == value } ?: CONFIRM
    }
}

data class AppSettings(
    val statsEnabled: Boolean = false,
    val updateDownloadMode: UpdateDownloadMode = UpdateDownloadMode.CONFIRM,
    val autoCheckUpdates: Boolean = false,
    val lastUpdateCheckAt: String? = null,
    val dismissedUpdateVersion: String? = null,
)

class SettingsStore(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::toSettings)

    suspend fun setStatsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[STATS_ENABLED] = enabled }
    }

    suspend fun setUpdateDownloadMode(mode: UpdateDownloadMode) {
        context.settingsDataStore.edit { it[UPDATE_DOWNLOAD_MODE] = mode.storedValue }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        context.settingsDataStore.edit { it[AUTO_CHECK_UPDATES] = enabled }
    }

    suspend fun setLastUpdateCheckAt(value: String?) {
        setOptionalString(LAST_UPDATE_CHECK_AT, value)
    }

    suspend fun setDismissedUpdateVersion(value: String?) {
        setOptionalString(DISMISSED_UPDATE_VERSION, value)
    }

    private suspend fun setOptionalString(key: Preferences.Key<String>, value: String?) {
        context.settingsDataStore.edit {
            if (value == null) it.remove(key) else it[key] = value
        }
    }

    private fun toSettings(preferences: Preferences) = AppSettings(
        statsEnabled = preferences[STATS_ENABLED] ?: false,
        updateDownloadMode = UpdateDownloadMode.fromStoredValue(preferences[UPDATE_DOWNLOAD_MODE]),
        autoCheckUpdates = preferences[AUTO_CHECK_UPDATES] ?: false,
        lastUpdateCheckAt = preferences[LAST_UPDATE_CHECK_AT],
        dismissedUpdateVersion = preferences[DISMISSED_UPDATE_VERSION],
    )

    private companion object {
        val STATS_ENABLED = booleanPreferencesKey("stats_enabled")
        val UPDATE_DOWNLOAD_MODE = stringPreferencesKey("update_download_mode")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val LAST_UPDATE_CHECK_AT = stringPreferencesKey("last_update_check_at")
        val DISMISSED_UPDATE_VERSION = stringPreferencesKey("dismissed_update_version")
    }
}
