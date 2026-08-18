package ru.alexandrgert.gamespuzzle.ui.play

object PlayTileChrome {
    const val UNLOCKED_OUTLINE_DP = 2

    fun shouldDrawUnlockedOutline(
        locked: Boolean,
        peek: Boolean,
        selected: Boolean,
        dragging: Boolean,
    ): Boolean = !peek && !locked && !selected && !dragging
}
