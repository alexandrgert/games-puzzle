package ru.alexandrgert.gamespuzzle.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UserPuzzle(
    val id: String,
    val file: String,
)

@Serializable
data class UserPuzzlesIndex(
    val puzzles: List<UserPuzzle> = emptyList(),
)

private val userPuzzlesJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal fun encodeUserPuzzlesIndex(index: UserPuzzlesIndex): String =
    userPuzzlesJson.encodeToString(index)

internal fun decodeUserPuzzlesIndex(text: String): UserPuzzlesIndex =
    runCatching { userPuzzlesJson.decodeFromString<UserPuzzlesIndex>(text) }
        .getOrDefault(UserPuzzlesIndex())

class UserPuzzlesStore(context: Context) {
    private val indexFile = File(context.filesDir, INDEX_FILE)

    suspend fun load(): List<UserPuzzle> = withContext(Dispatchers.IO) {
        readIndex().puzzles
    }

    suspend fun add(puzzle: UserPuzzle): List<UserPuzzle> = withContext(Dispatchers.IO) {
        val puzzles = readIndex().puzzles
            .filterNot { it.id == puzzle.id }
            .plus(puzzle)
        writeIndex(UserPuzzlesIndex(puzzles))
        puzzles
    }

    suspend fun remove(id: String): List<UserPuzzle> = withContext(Dispatchers.IO) {
        val puzzles = readIndex().puzzles.filterNot { it.id == id }
        writeIndex(UserPuzzlesIndex(puzzles))
        puzzles
    }

    private fun readIndex(): UserPuzzlesIndex {
        if (!indexFile.isFile) return UserPuzzlesIndex()
        return decodeUserPuzzlesIndex(
            runCatching { indexFile.readText() }.getOrDefault(""),
        )
    }

    private fun writeIndex(index: UserPuzzlesIndex) {
        val temporaryFile = File(indexFile.parentFile, "$INDEX_FILE.tmp")
        temporaryFile.writeText(encodeUserPuzzlesIndex(index))
        check(temporaryFile.renameTo(indexFile)) {
            "Не удалось сохранить список фотографий"
        }
    }

    private companion object {
        const val INDEX_FILE = "user_puzzles.json"
    }
}
