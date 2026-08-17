package ru.alexandrgert.gamespuzzle.data

import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.alexandrgert.gamespuzzle.domain.Semver

object UpdateChecker {
    fun check(client: OkHttpClient, current: Semver): UpdateCheckResult {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("User-Agent", "games-puzzle/${current.versionName()}")
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return offlineResult(current, "HTTP ${response.code}")
                }
                val body = response.body?.string()
                    ?: return offlineResult(current, "Empty response")
                evaluateUpdate(current, GithubReleaseJson.parseLatest(body))
            }
        }.getOrElse { error ->
            offlineResult(current, error.message)
        }
    }

    fun download(client: OkHttpClient, url: String, destination: File): Boolean {
        destination.delete()
        return runCatching {
            destination.parentFile?.mkdirs()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val body = response.body ?: return@use false
                destination.outputStream().use { output ->
                    body.byteStream().copyTo(output)
                }
                true
            }
        }.getOrDefault(false).also { succeeded ->
            if (!succeeded) destination.delete()
        }
    }

    private fun offlineResult(current: Semver, error: String?) = UpdateCheckResult(
        ok = false,
        error = error,
        current = current,
        latest = null,
        changelog = "",
        apkAssetUrl = null,
        updateAvailable = false,
    )

    private fun Semver.versionName(): String = "$major.$minor.$patch"

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/alexandrgert/games-puzzle/releases/latest"
    private const val USER_AGENT = "games-puzzle"
}
