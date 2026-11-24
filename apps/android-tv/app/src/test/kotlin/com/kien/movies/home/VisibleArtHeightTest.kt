package com.kien.movies.home

import org.junit.Assert.assertEquals
import org.junit.Test

/** The hero art's bottom edge follows the list scroll from the screen bottom to the slot's end. */
class VisibleArtHeightTest {
    @Test
    fun atTheTopOfThePageTheArtRunsToTheScreenBottom() {
        assertEquals(540f, visibleArtHeight(scrollOffset = 0f, slotHeight = 414.5f, fullHeight = 540f, retractDistance = 240f))
    }

    @Test
    fun halfwayThroughTheRetractScrollHalfTheArtBelowTheSlotShows() {
        assertEquals(477.25f, visibleArtHeight(scrollOffset = 120f, slotHeight = 414.5f, fullHeight = 540f, retractDistance = 240f))
    }

    @Test
    fun onceTheRowsAreCentredTheArtStopsAtTheSlotsEnd() {
        assertEquals(414.5f, visibleArtHeight(scrollOffset = 252f, slotHeight = 414.5f, fullHeight = 540f, retractDistance = 240f))
    }
}
