package com.kien.movies.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The hero art behind Home's list moves with the hero slot, also once the slot has left. */
class HeroScrollTest {
    @Test
    fun whileTheHeroIsFirstTheArtMovesByItsOffset() {
        assertEquals(120f, heroScroll(firstIndex = 0, firstOffset = 120, heroHeight = 829f))
    }

    @Test
    fun onceTheFirstRowIsFirstTheArtHasMovedTheWholeSlotPlusTheRowsOffset() {
        assertEquals(869f, heroScroll(firstIndex = 1, firstOffset = 40, heroHeight = 829f))
    }

    @Test
    fun furtherDownTheArtIsGone() {
        assertNull(heroScroll(firstIndex = 2, firstOffset = 0, heroHeight = 829f))
    }

    @Test
    fun theShownItemStaysInsideTheList() {
        assertEquals(2, shownHeroIndex(index = 5, count = 3))
        assertEquals(0, shownHeroIndex(index = 4, count = 0))
    }
}
