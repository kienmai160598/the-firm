package com.kien.movies.trailer

import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

/** The trailer covers its backdrop with no band, and the letterbox zoom only makes it larger (AC H8). */
class CoverSizeTest {
    @Test
    fun aSixteenByNineTrailerFillsASixteenByNineScreenExactly() {
        assertEquals(IntSize(1920, 1080), coverSize(1920, 1080, aspect = 16f / 9f, zoom = 1f))
    }

    @Test
    fun aWiderTrailerFillsTheHeightAndRunsOffTheSides() {
        assertEquals(IntSize(2592, 1080), coverSize(1920, 1080, aspect = 2.4f, zoom = 1f))
    }

    @Test
    fun aNarrowerTrailerFillsTheWidthAndRunsOffTopAndBottom() {
        assertEquals(IntSize(1920, 1440), coverSize(1920, 1080, aspect = 4f / 3f, zoom = 1f))
    }

    @Test
    fun theLetterboxZoomScalesBothSides() {
        assertEquals(IntSize(2304, 1296), coverSize(1920, 1080, aspect = 16f / 9f, zoom = 1.2f))
    }
}
