package ru.alexandrgert.gamespuzzle.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardEngineTest {
    @Test
    fun identityHasNoEmptyAndAllUnlocked() {
        val b = BoardEngine.identityUnlocked(GridSize.FIVE)
        assertTrue(b.tiles.size == 25)
        assertTrue(b.tiles.toList() == (0 until 25).toList())
        assertTrue(b.locked.all { !it })
    }

    @Test
    fun swapNonJoiningTilesReverts() {
        val start = Board(
            GridSize.FIVE,
            intArrayOf(
                1, 0, 2, 3, 4,
                5, 6, 7, 8, 9,
                10, 11, 12, 13, 14,
                15, 16, 17, 18, 19,
                20, 21, 22, 23, 24,
            ),
            BooleanArray(25),
        )
        val result = BoardEngine.trySwap(start, Cell(0, 0), Cell(4, 4))
        assertTrue(result is MoveResult.Reverted)
        assertArrayEquals(start.tiles, (result as MoveResult.Reverted).board.tiles)
    }

    @Test
    fun lockedTileNotAtHomeThrows() {
        val locked = BooleanArray(25).also { it[0] = true }
        val tiles = intArrayOf(
            1, 0, 2, 3, 4,
            5, 6, 7, 8, 9,
            10, 11, 12, 13, 14,
            15, 16, 17, 18, 19,
            20, 21, 22, 23, 24,
        )
        assertThrows(IllegalArgumentException::class.java) {
            Board(GridSize.FIVE, tiles, locked)
        }
    }

    @Test
    fun duplicateTileIdsThrow() {
        val tiles = intArrayOf(
            0, 0, 2, 3, 4,
            5, 6, 7, 8, 9,
            10, 11, 12, 13, 14,
            15, 16, 17, 18, 19,
            20, 21, 22, 23, 24,
        )
        assertThrows(IllegalArgumentException::class.java) {
            Board(GridSize.FIVE, tiles, BooleanArray(25))
        }
    }

    @Test
    fun swapLockedCellReverts() {
        val locked = BooleanArray(25).also { it[0] = true }
        val start = BoardEngine.identityUnlocked(GridSize.FIVE).let {
            Board(it.size, it.tiles, locked)
        }
        val result = BoardEngine.trySwap(start, Cell(0, 0), Cell(0, 1))
        assertTrue(result is MoveResult.Reverted)
    }

    @Test
    fun joiningPairAwayFromHomeSnapsAndLocks() {
        val tiles = IntArray(25) { it }
        tiles[20] = 0
        tiles[16] = 1
        tiles[0] = 20
        tiles[1] = 16
        val start = Board(GridSize.FIVE, tiles, BooleanArray(25))

        val result = BoardEngine.trySwap(start, Cell(3, 1), Cell(4, 1))

        assertTrue(result is MoveResult.Applied)
        val board = (result as MoveResult.Applied).board
        assertTrue(board.tiles[0] == 0)
        assertTrue(board.tiles[1] == 1)
        assertTrue(board.locked[0])
        assertTrue(board.locked[1])
        assertTrue(board.tiles[20] == 20)
        assertTrue(board.tiles[21] == 16)
    }

    @Test
    fun joiningPairWrongOrderDoesNotJoin() {
        val start = BoardEngine.identityUnlocked(GridSize.FIVE)

        val result = BoardEngine.trySwap(start, Cell(0, 0), Cell(0, 2))

        assertTrue(result is MoveResult.Reverted)
    }

    @Test
    fun attachToLockedNeighbourLocksInPlace() {
        val tiles = IntArray(25) { it }
        val locked = BooleanArray(25).also {
            it[0] = true
            it[1] = true
        }
        tiles[24] = 2
        tiles[2] = 24
        val start = Board(GridSize.FIVE, tiles, locked)
        val result = BoardEngine.trySwap(start, Cell(4, 4), Cell(0, 2))
        assertTrue(result is MoveResult.Applied)
        val board = (result as MoveResult.Applied).board
        assertTrue(board.tiles[2] == 2)
        assertTrue(board.locked[2])
        assertTrue(board.locked[0] && board.locked[1])
        assertTrue(board.tiles[24] == 24)
    }

    @Test
    fun cannotSwapOntoLockedTile() {
        val locked = BooleanArray(25).also { it[0] = true }
        val start = Board(GridSize.FIVE, IntArray(25) { it }, locked)
        val result = BoardEngine.trySwap(start, Cell(1, 0), Cell(0, 0))
        assertTrue(result is MoveResult.Reverted)
    }
}
