package ru.alexandrgert.gamespuzzle.ui.play

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayBoardGeometryTest {
    @Test
    fun portraitFitsAvailableHeightWithoutChangingProportions() {
        assertEquals(BoardDimensions(300f, 600f), fittedBoardSize(1000, 2000, 400f, 600f))
    }

    @Test
    fun lastTileIncludesRemainderPixelsOfPortraitImage() {
        assertEquals(TileBounds(667, 1334, 334, 667), tileBounds(1001, 2001, 8, 3))
    }

    @Test
    fun landscapeAndSquareImagesAlsoFitWithoutDistortion() {
        assertEquals(BoardDimensions(400f, 200f), fittedBoardSize(2000, 1000, 400f, 600f))
        assertEquals(BoardDimensions(400f, 400f), fittedBoardSize(1000, 1000, 400f, 600f))
    }

    @Test
    fun tileBoundariesCoverAllPixelsWithoutGapsOrOverlap() {
        assertEquals(TileBounds(0, 0, 333, 667), tileBounds(1001, 2001, 0, 3))
        assertEquals(TileBounds(333, 0, 334, 667), tileBounds(1001, 2001, 1, 3))
        assertEquals(TileBounds(667, 0, 334, 667), tileBounds(1001, 2001, 2, 3))
        assertEquals(TileBounds(0, 667, 333, 667), tileBounds(1001, 2001, 3, 3))
    }
}
