package ru.alexandrgert.gamespuzzle.ui.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.alexandrgert.gamespuzzle.data.UpdateCheckResult
import ru.alexandrgert.gamespuzzle.domain.Semver

class UpdatePromptTest {
    @Test
    fun `available update shows blocking download overlay while apk is loading`() {
        val state = buildUpdateDialogState(
            result = availableUpdate(),
            isDownloading = true,
        )

        assertTrue(state.canConfirmDownload)
        assertTrue(state.showDownloadOverlay)
    }

    @Test
    fun `idle available update does not show download overlay`() {
        val state = buildUpdateDialogState(
            result = availableUpdate(),
            isDownloading = false,
        )

        assertTrue(state.canConfirmDownload)
        assertFalse(state.showDownloadOverlay)
    }

    @Test
    fun `up to date dialog never shows download overlay`() {
        val state = buildUpdateDialogState(
            result = latestVersion(),
            isDownloading = true,
        )

        assertFalse(state.canConfirmDownload)
        assertFalse(state.showDownloadOverlay)
    }

    private fun availableUpdate() = UpdateCheckResult(
        ok = true,
        error = null,
        current = Semver(0, 5, 0),
        latest = Semver(0, 5, 1),
        changelog = "notes",
        apkAssetUrl = "https://example.test/app.apk",
        updateAvailable = true,
    )

    private fun latestVersion() = availableUpdate().copy(
        latest = Semver(0, 5, 0),
        updateAvailable = false,
        apkAssetUrl = null,
    )
}
