package ru.alexandrgert.gamespuzzle.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UserPuzzlesStoreTest {
    @Test
    fun indexRoundTripsWithRequiredShape() {
        val index = UserPuzzlesIndex(
            puzzles = listOf(
                UserPuzzle(id = "user:abc-123", file = "user/abc-123.webp"),
            ),
        )

        val encoded = encodeUserPuzzlesIndex(index)

        assertEquals(index, decodeUserPuzzlesIndex(encoded))
        assertEquals(
            """{"puzzles":[{"id":"user:abc-123","file":"user/abc-123.webp"}]}""",
            encoded,
        )
    }

    @Test
    fun malformedIndexIsTreatedAsEmpty() {
        assertEquals(UserPuzzlesIndex(), decodeUserPuzzlesIndex("not json"))
    }
}
