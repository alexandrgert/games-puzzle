package ru.alexandrgert.gamespuzzle.data

import ru.alexandrgert.gamespuzzle.domain.Semver
import ru.alexandrgert.gamespuzzle.domain.parseSemver

data class UpdateCheckDecision(
    val showDialog: Boolean,
    val downloadWhenPromptShown: Boolean,
    val showOfflineSnackbar: Boolean,
    val recordCheckTimestamp: Boolean,
    val isManualPrompt: Boolean,
)

fun shouldRunAutoCheck(enabled: Boolean): Boolean = enabled

fun shouldShowStartupPrompt(
    result: UpdateCheckResult,
    dismissedVersion: String?,
): Boolean {
    if (!result.ok || !result.updateAvailable) return false
    val latest = result.latest ?: return false
    val dismissed = parseDismissedVersion(dismissedVersion) ?: return true
    return latest > dismissed
}

fun shouldPersistDismissed(isManualPrompt: Boolean, postpone: Boolean): Boolean =
    !isManualPrompt && postpone

fun shouldShowUpdatePromptOnScreen(isManualPrompt: Boolean, onCatalog: Boolean): Boolean =
    isManualPrompt || onCatalog

fun decideAfterUpdateCheck(
    manual: Boolean,
    result: UpdateCheckResult,
    downloadMode: UpdateDownloadMode,
    dismissedVersion: String?,
): UpdateCheckDecision {
    if (manual) {
        val showDialog = result.ok
        return UpdateCheckDecision(
            showDialog = showDialog,
            downloadWhenPromptShown = showDialog &&
                result.updateAvailable &&
                downloadMode == UpdateDownloadMode.IMMEDIATE,
            showOfflineSnackbar = !result.ok,
            recordCheckTimestamp = result.ok,
            isManualPrompt = true,
        )
    }
    val showDialog = shouldShowStartupPrompt(result, dismissedVersion)
    return UpdateCheckDecision(
        showDialog = showDialog,
        downloadWhenPromptShown = showDialog &&
            result.updateAvailable &&
            downloadMode == UpdateDownloadMode.IMMEDIATE,
        showOfflineSnackbar = false,
        recordCheckTimestamp = result.ok,
        isManualPrompt = false,
    )
}

fun versionLabel(semver: Semver): String = "${semver.major}.${semver.minor}.${semver.patch}"

private fun parseDismissedVersion(raw: String?): Semver? {
    if (raw.isNullOrBlank()) return null
    return runCatching { parseSemver(raw) }.getOrNull()
}
