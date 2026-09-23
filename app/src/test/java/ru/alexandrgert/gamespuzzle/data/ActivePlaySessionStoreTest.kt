package ru.alexandrgert.gamespuzzle.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.PlaySessionSnapshot

@OptIn(ExperimentalCoroutinesApi::class)
class ActivePlaySessionStoreTest {
    @Test
    fun publicContractRoundTripsAndClearsEverySessionField() = runTest {
        val encoded = mutableMapOf<String, String>()
        val store: ActivePlaySessionStore = storeFor(encoded, UnconfinedTestDispatcher(testScheduler))
        val snapshot = snapshot()

        store.save("game", snapshot)
        val restored = store.load("game")!!

        assertTrue(encoded.getValue("game").contains("\"schemaVersion\":1"))
        assertEquals(snapshot.sizeN, restored.sizeN)
        assertEquals(snapshot.statsEnabled, restored.statsEnabled)
        assertTrue(snapshot.tiles.contentEquals(restored.tiles))
        assertTrue(snapshot.locked.contentEquals(restored.locked))
        assertEquals(snapshot.selectedIndex, restored.selectedIndex)
        assertEquals(snapshot.moves, restored.moves)
        assertEquals(snapshot.peek, restored.peek)
        assertEquals(snapshot.elapsedMs, restored.elapsedMs)
        assertEquals(snapshot.completed, restored.completed)

        store.clear("game")

        assertNull(store.load("game"))
        assertFalse(encoded.containsKey("game"))
    }

    @Test
    fun loadOfCorruptedEntryReturnsNullAndRemovesIt() = runTest {
        val encoded = mutableMapOf("game" to "{not-json")
        val store: ActivePlaySessionStore = storeFor(encoded, UnconfinedTestDispatcher(testScheduler))

        assertNull(store.load("game"))
        assertFalse(encoded.containsKey("game"))
    }

    @Test
    fun publicLoadTreatsReadFailureAsMissingWithoutAttemptingCleanup() = runTest {
        var removals = 0
        val store: ActivePlaySessionStore = SerializedActivePlaySessionStore(
            read = { throw IllegalStateException("DataStore unavailable") },
            write = { _, _ -> },
            remove = { removals++ },
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        assertNull(store.load("game"))
        assertEquals(0, removals)
    }

    @Test
    fun serializationWaitsForTheConfiguredIoDispatcher() = runTest {
        val encoded = mutableMapOf<String, String>()
        val store: ActivePlaySessionStore = storeFor(
            encoded,
            StandardTestDispatcher(testScheduler),
        )

        val save = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.save("game", snapshot())
        }

        assertFalse(save.isCompleted)
        assertFalse(encoded.containsKey("game"))
        runCurrent()
        assertTrue(save.isCompleted)
        assertTrue(encoded.containsKey("game"))
    }

    private fun storeFor(
        encoded: MutableMap<String, String>,
        dispatcher: CoroutineDispatcher,
    ) = SerializedActivePlaySessionStore(
        read = { encoded[it] },
        write = { key, value -> encoded[key] = value },
        remove = { encoded.remove(it) },
        ioDispatcher = dispatcher,
    )

    private fun snapshot() = PlaySessionSnapshot(
        sizeN = 5,
        statsEnabled = true,
        tiles = IntArray(25) { 24 - it },
        locked = BooleanArray(25) { it % 2 == 0 },
        selectedIndex = 7,
        moves = 13,
        peek = true,
        elapsedMs = 4_200L,
        completed = false,
    )
}
