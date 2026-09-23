package ru.alexandrgert.gamespuzzle.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.alexandrgert.gamespuzzle.domain.PlaySessionSnapshot

private val Context.activePlaySessionDataStore by preferencesDataStore(name = "active_play_sessions")

interface ActivePlaySessionStore {
    suspend fun load(key: String): PlaySessionSnapshot?
    suspend fun save(key: String, snapshot: PlaySessionSnapshot)
    suspend fun clear(key: String)
}

@Serializable
private data class StoredPlaySession(
    val schemaVersion: Int,
    val snapshot: PlaySessionSnapshot,
)

class DataStoreActivePlaySessionStore(context: Context) : ActivePlaySessionStore {
    private val delegate = SerializedActivePlaySessionStore(
        read = { key ->
            context.activePlaySessionDataStore.data.first()[preferenceKey(key)]
        },
        write = { key, encoded ->
            context.activePlaySessionDataStore.edit { it[preferenceKey(key)] = encoded }
        },
        remove = { key ->
            context.activePlaySessionDataStore.edit { it.remove(preferenceKey(key)) }
        },
    )

    override suspend fun load(key: String): PlaySessionSnapshot? = delegate.load(key)

    override suspend fun save(key: String, snapshot: PlaySessionSnapshot) =
        delegate.save(key, snapshot)

    override suspend fun clear(key: String) = delegate.clear(key)
}

internal class SerializedActivePlaySessionStore(
    private val read: suspend (String) -> String?,
    private val write: suspend (String, String) -> Unit,
    private val remove: suspend (String) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ActivePlaySessionStore {
    override suspend fun load(key: String): PlaySessionSnapshot? = withContext(ioDispatcher) {
        val encoded = try {
            read(key)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@withContext null
        } ?: return@withContext null
        try {
            decodeActivePlaySession(encoded)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            try {
                remove(key)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A malformed value remains harmless even if cleanup cannot be persisted yet.
            }
            null
        }
    }

    override suspend fun save(key: String, snapshot: PlaySessionSnapshot) = withContext(ioDispatcher) {
        write(key, encodeActivePlaySession(snapshot))
    }

    override suspend fun clear(key: String) = withContext(ioDispatcher) {
        remove(key)
    }
}

private const val ACTIVE_SESSION_SCHEMA_VERSION = 1
private val activeSessionJson = Json { ignoreUnknownKeys = true }

private fun preferenceKey(key: String) = stringPreferencesKey("session:$key")

private fun encodeActivePlaySession(snapshot: PlaySessionSnapshot): String =
    activeSessionJson.encodeToString(StoredPlaySession(ACTIVE_SESSION_SCHEMA_VERSION, snapshot))

private fun decodeActivePlaySession(encoded: String): PlaySessionSnapshot {
    val stored = activeSessionJson.decodeFromString<StoredPlaySession>(encoded)
    require(stored.schemaVersion == ACTIVE_SESSION_SCHEMA_VERSION)
    return stored.snapshot
}
