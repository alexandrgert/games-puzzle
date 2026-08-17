package ru.alexandrgert.gamespuzzle.domain

enum class GridSize(val n: Int) {
    FIVE(5),
    SIX(6),
    EIGHT(8),
    TEN(10),
    TWELVE(12),
}

data class Cell(val row: Int, val col: Int) {
    fun inBounds(n: Int): Boolean = row in 0 until n && col in 0 until n
    fun index(n: Int): Int = row * n + col

    companion object {
        fun fromIndex(index: Int, n: Int): Cell = Cell(index / n, index % n)
    }
}
