package ru.alexandrgert.gamespuzzle.ui.update

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import ru.alexandrgert.gamespuzzle.data.UpdateCheckResult
import ru.alexandrgert.gamespuzzle.domain.Semver

@RunWith(AndroidJUnit4::class)
class UpdatePromptUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun downloadingAvailableUpdateShowsOverlayAndDisablesDownloadButton() {
        composeRule.setContent {
            MaterialTheme {
                UpdateResultDialog(
                    result = availableUpdate(),
                    isDownloading = true,
                    onDownload = {},
                    onDismiss = {},
                    onPostpone = {},
                )
            }
        }

        assertEquals(
            1,
            composeRule.onAllNodesWithTag(UPDATE_DOWNLOAD_OVERLAY_TAG).fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithText("Скачать").assertIsNotEnabled()
    }

    @Test
    fun idleDialogDoesNotShowOverlay() {
        composeRule.setContent {
            MaterialTheme {
                UpdateResultDialog(
                    result = availableUpdate(),
                    isDownloading = false,
                    onDownload = {},
                    onDismiss = {},
                    onPostpone = {},
                )
            }
        }

        assertEquals(
            0,
            composeRule.onAllNodesWithTag(UPDATE_DOWNLOAD_OVERLAY_TAG).fetchSemanticsNodes().size,
        )
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
}
