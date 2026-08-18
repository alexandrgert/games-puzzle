package ru.alexandrgert.gamespuzzle.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.Semver

class StartupUpdatePolicyTest {
    private val current = Semver(0, 2, 0)
    private val latest = Semver(0, 3, 0)

    @Test
    fun autoCheckRunsOnlyWhenEnabled() {
        assertFalse(shouldRunAutoCheck(enabled = false))
        assertTrue(shouldRunAutoCheck(enabled = true))
    }

    @Test
    fun startupPromptShowsOnlyForNewUndismissedRelease() {
        assertFalse(shouldShowStartupPrompt(offline(), dismissedVersion = null))
        assertFalse(shouldShowStartupPrompt(upToDate(), dismissedVersion = null))
        assertTrue(shouldShowStartupPrompt(available(), dismissedVersion = null))
        assertFalse(shouldShowStartupPrompt(available(), dismissedVersion = "0.3.0"))
        assertFalse(shouldShowStartupPrompt(available(), dismissedVersion = "v0.3.0"))
        assertTrue(shouldShowStartupPrompt(available(), dismissedVersion = "0.2.0"))
        assertFalse(shouldShowStartupPrompt(available(), dismissedVersion = "0.4.0"))
    }

    @Test
    fun persistDismissedOnlyOnStartupCancel() {
        assertTrue(shouldPersistDismissed(isManualPrompt = false, postpone = true))
        assertFalse(shouldPersistDismissed(isManualPrompt = false, postpone = false))
        assertFalse(shouldPersistDismissed(isManualPrompt = true, postpone = true))
        assertFalse(shouldPersistDismissed(isManualPrompt = true, postpone = false))
    }

    @Test
    fun startupDialogOnlyOnCatalog() {
        assertTrue(shouldShowUpdatePromptOnScreen(isManualPrompt = false, onCatalog = true))
        assertFalse(shouldShowUpdatePromptOnScreen(isManualPrompt = false, onCatalog = false))
        assertTrue(shouldShowUpdatePromptOnScreen(isManualPrompt = true, onCatalog = false))
    }

    @Test
    fun decideAfterCheck_manualOfflineShowsSnackbar() {
        val decision = decideAfterUpdateCheck(
            manual = true,
            result = offline(),
            dismissedVersion = null,
        )
        assertEquals(
            UpdateCheckDecision(
                showDialog = false,
                downloadWhenPromptShown = false,
                showOfflineSnackbar = true,
                recordCheckTimestamp = false,
                isManualPrompt = true,
            ),
            decision,
        )
    }

    @Test
    fun decideAfterCheck_manualLatestShowsDialogWithoutDownload() {
        val decision = decideAfterUpdateCheck(
            manual = true,
            result = upToDate(),
            dismissedVersion = null,
        )
        assertTrue(decision.showDialog)
        assertFalse(decision.downloadWhenPromptShown)
        assertFalse(decision.showOfflineSnackbar)
        assertTrue(decision.recordCheckTimestamp)
        assertTrue(decision.isManualPrompt)
    }

    @Test
    fun decideAfterCheck_manualAvailableDoesNotDownloadUntilConfirmed() {
        val decision = decideAfterUpdateCheck(
            manual = true,
            result = available(),
            dismissedVersion = null,
        )
        assertTrue(decision.showDialog)
        assertFalse(decision.downloadWhenPromptShown)
        assertTrue(decision.isManualPrompt)
    }

    @Test
    fun decideAfterCheck_startupAvailableDoesNotDownloadUntilConfirmed() {
        val decision = decideAfterUpdateCheck(
            manual = false,
            result = available(),
            dismissedVersion = null,
        )
        assertTrue(decision.showDialog)
        assertFalse(decision.downloadWhenPromptShown)
        assertFalse(decision.showOfflineSnackbar)
        assertTrue(decision.recordCheckTimestamp)
        assertFalse(decision.isManualPrompt)
    }

    @Test
    fun decideAfterCheck_startupDismissedOrOfflineIsSilent() {
        val dismissed = decideAfterUpdateCheck(
            manual = false,
            result = available(),
            dismissedVersion = "0.3.0",
        )
        assertFalse(dismissed.showDialog)
        assertFalse(dismissed.downloadWhenPromptShown)
        assertFalse(dismissed.showOfflineSnackbar)

        val offline = decideAfterUpdateCheck(
            manual = false,
            result = offline(),
            dismissedVersion = null,
        )
        assertFalse(offline.showDialog)
        assertFalse(offline.showOfflineSnackbar)
        assertFalse(offline.recordCheckTimestamp)
    }

    private fun available() = UpdateCheckResult(
        ok = true,
        error = null,
        current = current,
        latest = latest,
        changelog = "notes",
        apkAssetUrl = "https://example.test/app.apk",
        updateAvailable = true,
    )

    private fun upToDate() = available().copy(
        latest = current,
        updateAvailable = false,
    )

    private fun offline() = UpdateCheckResult(
        ok = false,
        error = "offline",
        current = current,
        latest = null,
        changelog = "",
        apkAssetUrl = null,
        updateAvailable = false,
    )
}
