package ru.alexandrgert.gamespuzzle.ui.play

import kotlin.math.min

internal data class BoardDimensions(val width: Float, val height: Float)

/** Fit the complete image inside the space left after controls and statistics. */
internal fun fittedBoardSize(
    imageWidth: Int,
    imageHeight: Int,
    availableWidth: Float,
    availableHeight: Float,
): BoardDimensions {
    require(imageWidth > 0 && imageHeight > 0)
    if (availableWidth <= 0f || availableHeight <= 0f) return BoardDimensions(0f, 0f)
    val scale = min(availableWidth / imageWidth, availableHeight / imageHeight)
    return BoardDimensions(imageWidth * scale, imageHeight * scale)
}

internal data class TileBounds(val left: Int, val top: Int, val width: Int, val height: Int)

/** Integer boundaries share edges, including the image's final row and column. */
internal fun tileBounds(width: Int, height: Int, tileId: Int, n: Int): TileBounds {
    require(n > 0 && width >= n && height >= n)
    require(tileId in 0 until n * n)
    val row = tileId / n
    val col = tileId % n
    val left = (col.toLong() * width / n).toInt()
    val top = (row.toLong() * height / n).toInt()
    val right = ((col + 1L) * width / n).toInt()
    val bottom = ((row + 1L) * height / n).toInt()
    return TileBounds(left, top, right - left, bottom - top)
}
