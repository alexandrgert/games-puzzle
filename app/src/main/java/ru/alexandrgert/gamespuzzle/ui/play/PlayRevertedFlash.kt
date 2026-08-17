package ru.alexandrgert.gamespuzzle.ui.play

import ru.alexandrgert.gamespuzzle.domain.Cell

fun isRevertedFlashCell(
    cell: Cell,
    lastReverted: Boolean,
    revertedA: Cell?,
    revertedB: Cell?,
): Boolean = lastReverted && (cell == revertedA || cell == revertedB)
