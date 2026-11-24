package com.kien.movies.design

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Apple's concentric rule: outer radius = inner radius + padding. */
class ConcentricTest {
    @Test
    fun anInnerShapeLosesTheInsetFromTheOuterRadius() {
        assertEquals(8.dp, concentricRadius(outer = 20.dp, inset = 12.dp))
    }

    @Test
    fun anInsetLargerThanTheOuterRadiusNeverGivesANegativeRadius() {
        assertEquals(0.dp, concentricRadius(outer = 12.dp, inset = 18.dp))
    }

    @Test
    fun anInsetLargerThanTheOuterRadiusFallsBackToTheMinimum() {
        assertEquals(7.dp, concentricRadius(outer = 12.dp, inset = 18.dp, minimum = 7.dp))
    }

    @Test
    fun theMinimumDoesNotShrinkAConcentricRadius() {
        assertEquals(8.dp, concentricRadius(outer = 20.dp, inset = 12.dp, minimum = 3.dp))
    }

    @Test
    fun aContainerAddsItsPaddingToTheInnerRadius() {
        assertEquals(48.dp, containerRadius(inner = 16.dp, padding = 32.dp))
    }
}
