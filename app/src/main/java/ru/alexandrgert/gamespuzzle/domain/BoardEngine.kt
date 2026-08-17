package ru.alexandrgert.gamespuzzle.domain

object BoardEngine {
    fun identityUnlocked(size: GridSize): Board {
        val n = size.n
        val count = n * n
        return Board(size, IntArray(count) { it }, BooleanArray(count))
    }

    fun trySwap(board: Board, a: Cell, b: Cell): MoveResult {
        val n = board.n
        if (a == b || !a.inBounds(n) || !b.inBounds(n)) return MoveResult.Reverted(board)
        if (board.isLockedCell(a) || board.isLockedCell(b)) return MoveResult.Reverted(board)
        return MoveResult.Reverted(board)
    }
}
