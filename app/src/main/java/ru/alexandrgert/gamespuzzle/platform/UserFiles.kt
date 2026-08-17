package ru.alexandrgert.gamespuzzle.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.OutputStream
import java.util.UUID
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.alexandrgert.gamespuzzle.data.UserPuzzle

sealed interface UserPhotoImportResult {
    data class Success(val puzzle: UserPuzzle) : UserPhotoImportResult

    data object TooSmall : UserPhotoImportResult

    data object OpenError : UserPhotoImportResult
}

class UserFiles(private val context: Context) {
    private val userDirectory = File(context.filesDir, USER_DIRECTORY)

    suspend fun importPhoto(uri: Uri): UserPhotoImportResult = withContext(Dispatchers.IO) {
        val source = runCatching {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }.getOrNull() ?: return@withContext UserPhotoImportResult.OpenError

        try {
            val shortest = min(source.width, source.height)
            val outputSide = importSquareOutputSide(shortest)
                ?: return@withContext UserPhotoImportResult.TooSmall

            val idPart = UUID.randomUUID().toString()
            val target = File(userDirectory, "$idPart.webp")
            val square = Bitmap.createBitmap(
                source,
                (source.width - shortest) / 2,
                (source.height - shortest) / 2,
                shortest,
                shortest,
            )
            var playBitmap = square
            try {
                if (outputSide < shortest) {
                    playBitmap = Bitmap.createScaledBitmap(square, outputSide, outputSide, true)
                }
                userDirectory.mkdirs()
                val saved = runCatching {
                    target.outputStream().buffered().use { output ->
                        compressWebp(playBitmap, output)
                    }
                }.getOrDefault(false)
                if (!saved) {
                    target.delete()
                    return@withContext UserPhotoImportResult.OpenError
                }
            } finally {
                if (playBitmap !== square) playBitmap.recycle()
                if (square !== source) square.recycle()
            }

            UserPhotoImportResult.Success(
                UserPuzzle(
                    id = "user:$idPart",
                    file = "$USER_DIRECTORY/$idPart.webp",
                ),
            )
        } catch (_: RuntimeException) {
            UserPhotoImportResult.OpenError
        } finally {
            source.recycle()
        }
    }

    fun load(id: String): Bitmap? {
        val idPart = id.removePrefix(USER_ID_PREFIX)
        if (!id.startsWith(USER_ID_PREFIX) || !UUID_PATTERN.matches(idPart)) return null
        return BitmapFactory.decodeFile(File(userDirectory, "$idPart.webp").absolutePath)
    }

    suspend fun delete(puzzle: UserPuzzle): Boolean = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, puzzle.file)
        val safeParent = runCatching { file.canonicalFile.parentFile == userDirectory.canonicalFile }
            .getOrDefault(false)
        safeParent && (!file.exists() || file.delete())
    }

    @Suppress("DEPRECATION")
    private fun compressWebp(bitmap: Bitmap, output: OutputStream): Boolean =
        bitmap.compress(Bitmap.CompressFormat.WEBP, WEBP_QUALITY, output)

    private companion object {
        const val USER_DIRECTORY = "user"
        const val USER_ID_PREFIX = "user:"
        const val WEBP_QUALITY = 90
        val UUID_PATTERN = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-" +
                "[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}",
        )
    }
}
