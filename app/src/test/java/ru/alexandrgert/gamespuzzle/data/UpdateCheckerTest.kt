package ru.alexandrgert.gamespuzzle.data

import java.io.File
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.alexandrgert.gamespuzzle.domain.Semver

class UpdateCheckerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `check sends release request and evaluates update`() {
        var requestedUrl: String? = null
        var userAgent: String? = null
        val client = clientResponding(
            code = 200,
            body = """
                {
                  "tag_name": "v1.2.0",
                  "body": "Что нового",
                  "assets": [{
                    "name": "games-puzzle.apk",
                    "browser_download_url": "https://example.test/games-puzzle.apk"
                  }]
                }
            """.trimIndent(),
            inspect = {
                requestedUrl = it.request().url.toString()
                userAgent = it.request().header("User-Agent")
            },
        )

        val result = UpdateChecker.check(client, Semver(1, 1, 0))

        assertEquals(
            "https://api.github.com/repos/alexandrgert/games-puzzle/releases/latest",
            requestedUrl,
        )
        assertEquals("games-puzzle/1.1.0", userAgent)
        assertTrue(result.ok)
        assertTrue(result.updateAvailable)
        assertEquals("Что нового", result.changelog)
        assertEquals("https://example.test/games-puzzle.apk", result.apkAssetUrl)
    }

    @Test
    fun `check returns offline result without response body on http error`() {
        val client = clientResponding(code = 503, body = "server body")

        val result = UpdateChecker.check(client, Semver(1, 1, 0))

        assertFalse(result.ok)
        assertEquals("", result.changelog)
        assertNull(result.latest)
        assertFalse(result.updateAvailable)
    }

    @Test
    fun `check returns offline result on network error`() {
        val client = OkHttpClient.Builder()
            .addInterceptor { throw IOException("offline") }
            .build()

        val result = UpdateChecker.check(client, Semver(1, 1, 0))

        assertFalse(result.ok)
        assertEquals("", result.changelog)
    }

    @Test
    fun `download writes apk to destination`() {
        val destination = temporaryFolder.newFolder("updates").resolve("games-puzzle.apk")
        val client = clientResponding(code = 200, body = "apk contents")

        val downloaded = UpdateChecker.download(
            client = client,
            url = "https://example.test/games-puzzle.apk",
            destination = destination,
        )

        assertTrue(downloaded)
        assertEquals("apk contents", destination.readText())
    }

    @Test
    fun `download deletes partial destination on failure`() {
        val destination = File(temporaryFolder.newFolder("updates"), "games-puzzle.apk")
        destination.writeText("old partial")
        val client = clientResponding(code = 500, body = "failure")

        val downloaded = UpdateChecker.download(
            client = client,
            url = "https://example.test/games-puzzle.apk",
            destination = destination,
        )

        assertFalse(downloaded)
        assertFalse(destination.exists())
    }

    private fun clientResponding(
        code: Int,
        body: String,
        inspect: (Interceptor.Chain) -> Unit = {},
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            inspect(chain)
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }
        .build()
}
