package ru.alexandrgert.gamespuzzle.ui.play

import ru.alexandrgert.gamespuzzle.domain.Cell

fun cellAt(x: Float, y: Float, n: Int, widthPx: Float, heightPx: Float): Cell? {
    if (n <= 0 || widthPx <= 0f || heightPx <= 0f) return null
    if (x < 0f || y < 0f || x >= widthPx || y >= heightPx) return null
    val col = (x / (widthPx / n)).toInt().coerceIn(0, n - 1)
    val row = (y / (heightPx / n)).toInt().coerceIn(0, n - 1)
    return Cell(row, col)
}
