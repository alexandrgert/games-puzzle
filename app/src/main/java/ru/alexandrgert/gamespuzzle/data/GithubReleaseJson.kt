package ru.alexandrgert.gamespuzzle.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.alexandrgert.gamespuzzle.domain.Semver
import ru.alexandrgert.gamespuzzle.domain.parseSemver

data class ReleaseInfo(
    val tag: String,
    val body: String,
    val apkUrl: String?,
)

data class UpdateCheckResult(
    val ok: Boolean,
    val error: String?,
    val current: Semver,
    val latest: Semver?,
    val changelog: String,
    val apkAssetUrl: String?,
    val updateAvailable: Boolean,
)

object GithubReleaseJson {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parseLatest(json: String): ReleaseInfo {
        val payload = this.json.decodeFromString<GithubReleasePayload>(json)
        return ReleaseInfo(
            tag = payload.tag,
            body = payload.body,
            apkUrl = payload.assets.firstOrNull { it.name.endsWith(".apk") }?.downloadUrl,
        )
    }
}

fun evaluateUpdate(current: Semver, release: ReleaseInfo): UpdateCheckResult {
    val latest = parseSemver(release.tag)
    return UpdateCheckResult(
        ok = true,
        error = null,
        current = current,
        latest = latest,
        changelog = release.body,
        apkAssetUrl = release.apkUrl,
        updateAvailable = latest > current,
    )
}

@Serializable
private data class GithubReleasePayload(
    @SerialName("tag_name")
    val tag: String,
    val body: String = "",
    val assets: List<GithubAssetPayload> = emptyList(),
)

@Serializable
private data class GithubAssetPayload(
    val name: String,
    @SerialName("browser_download_url")
    val downloadUrl: String,
)
