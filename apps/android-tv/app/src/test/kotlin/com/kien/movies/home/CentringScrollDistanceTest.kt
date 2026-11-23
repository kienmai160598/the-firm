package com.kien.movies.home

import org.junit.Assert.assertEquals
import org.junit.Test

/** Home's list centres a focused card on the screen and brings the hero slot back to the top. */
class CentringScrollDistanceTest {
    @Test
    fun aCardBelowTheCentreScrollsUntilItsCentreIsTheScreensCentre() {
        assertEquals(250f, centringScrollDistance(offset = 465f, size = 110f, containerSize = 540f))
    }

    @Test
    fun aCardAboveTheCentreScrollsBack() {
        assertEquals(-258f, centringScrollDistance(offset = -85.5f, size = 191f, containerSize = 540f))
    }

    @Test
    fun theHeroSlotIsAlignedToTheTop() {
        assertEquals(-251f, centringScrollDistance(offset = -251f, size = 414.5f, containerSize = 540f))
    }
}
