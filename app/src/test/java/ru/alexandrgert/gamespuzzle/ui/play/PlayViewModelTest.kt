package ru.alexandrgert.gamespuzzle.ui.play

import java.util.Random
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.Board
import ru.alexandrgert.gamespuzzle.domain.BoardEngine
import ru.alexandrgert.gamespuzzle.domain.Cell
import ru.alexandrgert.gamespuzzle.domain.GridSize
import ru.alexandrgert.gamespuzzle.domain.MoveResult

class PlayViewModelTest {
    @Test
    fun repeatedStartKeepsTheActiveBoard() {
        val viewModel = PlayViewModel()
        viewModel.start(GridSize.FIVE, Random(1L))
        val originalBoard = viewModel.state!!.board

        viewModel.start(GridSize.FIVE, Random(2L))

        assertSame(originalBoard, viewModel.state!!.board)
    }

    @Test
    fun clearsRevertedSwapSignalAfterUiHandlesIt() {
        val viewModel = PlayViewModel()
        viewModel.start(GridSize.FIVE, Random(3L))
        val (first, second) = findRevertedPair(viewModel.state!!.board)

        viewModel.onCell(first)
        viewModel.onCell(second)
        assertTrue(viewModel.state!!.lastReverted)

        viewModel.clearLastReverted()

        assertFalse(viewModel.state!!.lastReverted)
    }

    private fun findRevertedPair(
        board: Board,
    ): Pair<Cell, Cell> {
        val cells = (0 until board.n * board.n).map { Cell.fromIndex(it, board.n) }
        for (first in cells) {
            for (second in cells) {
                if (first != second && BoardEngine.trySwap(board, first, second) is MoveResult.Reverted) {
                    return first to second
                }
            }
        }
        error("No reverted pair")
    }
}
