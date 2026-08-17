package ru.alexandrgert.gamespuzzle.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.Semver

class GithubReleaseJsonTest {
    @Test
    fun parsesLatestReleaseFixtureAndSelectsFirstApk() {
        val text = checkNotNull(javaClass.getResource("/github_release_latest.json")).readText()

        val release = GithubReleaseJson.parseLatest(text)

        assertEquals("v1.2.3", release.tag)
        assertEquals("Fixes and improvements.", release.body)
        assertEquals("https://example.com/downloads/games-puzzle-v1.2.3.apk", release.apkUrl)
    }

    @Test
    fun reportsEqualVersionAsUnavailableAndCopiesChangelog() {
        val release = ReleaseInfo(
            tag = "v1.2.3",
            body = "Already installed.",
            apkUrl = "https://example.com/app.apk",
        )

        val result = evaluateUpdate(Semver(1, 2, 3), release)

        assertTrue(result.ok)
        assertEquals(null, result.error)
        assertEquals(Semver(1, 2, 3), result.current)
        assertEquals(Semver(1, 2, 3), result.latest)
        assertEquals("Already installed.", result.changelog)
        assertEquals("https://example.com/app.apk", result.apkAssetUrl)
        assertFalse(result.updateAvailable)
    }
}
