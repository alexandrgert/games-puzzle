package ru.alexandrgert.gamespuzzle.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import ru.alexandrgert.gamespuzzle.domain.BestRecord
import ru.alexandrgert.gamespuzzle.domain.mergeRecord

private val Context.recordsDataStore by preferencesDataStore(name = "records")

data class RecordUpdate(
    val record: BestRecord,
    val improvedTime: Boolean,
    val improvedMoves: Boolean,
)

interface RecordSaver {
    suspend fun save(puzzleId: String, n: Int, timeMs: Long, moves: Int): RecordUpdate
}

class RecordsStore(private val context: Context) : RecordSaver {
    suspend fun load(puzzleId: String, n: Int): BestRecord? {
        val value = context.recordsDataStore.data.first()[recordKey(puzzleId, n)]
        return parseRecord(value)
    }

    override suspend fun save(
        puzzleId: String,
        n: Int,
        timeMs: Long,
        moves: Int,
    ): RecordUpdate {
        var result: RecordUpdate? = null
        context.recordsDataStore.edit { preferences ->
            val key = recordKey(puzzleId, n)
            val previous = parseRecord(preferences[key])
            val merged = mergeRecord(previous, timeMs, moves)
            preferences[key] = serializeRecord(merged)
            result = RecordUpdate(
                record = merged,
                improvedTime = previous?.bestTimeMs == null || timeMs < previous.bestTimeMs,
                improvedMoves = previous?.bestMoves == null || moves < previous.bestMoves,
            )
        }
        return checkNotNull(result)
    }

    suspend fun delete(puzzleId: String) {
        context.recordsDataStore.edit { preferences ->
            preferences.remove(recordKey(puzzleId, 5))
            preferences.remove(recordKey(puzzleId, 6))
        }
    }

    private fun recordKey(puzzleId: String, n: Int) =
        stringPreferencesKey("rec:$puzzleId:$n")
}

fun serializeRecord(record: BestRecord): String =
    "${record.bestTimeMs.orEmpty()},${record.bestMoves.orEmpty()}"

fun parseRecord(value: String?): BestRecord? {
    val parts = value?.split(',') ?: return null
    if (parts.size != 2) return null
    val time = parts[0].toLongOrNull()?.takeIf { it >= 0 } ?: return null
    val moves = parts[1].toIntOrNull()?.takeIf { it >= 0 } ?: return null
    return BestRecord(time, moves)
}

private fun Long?.orEmpty(): String = this?.toString().orEmpty()

private fun Int?.orEmpty(): String = this?.toString().orEmpty()
