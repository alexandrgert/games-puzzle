package ru.alexandrgert.gamespuzzle.platform

const val IMPORT_MIN_SIDE = 512
const val IMPORT_MAX_SIDE = 1200

fun importSquareOutputSide(shortestSide: Int): Int? {
    if (shortestSide < IMPORT_MIN_SIDE) return null
    return minOf(shortestSide, IMPORT_MAX_SIDE)
}
