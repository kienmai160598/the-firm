package com.kien.movies.design

import org.junit.Assert.assertEquals
import org.junit.Test

/** A focused card scrolls only as far as needed and never comes to rest under the closed rail. */
class InsetScrollDistanceTest {
    @Test
    fun aCardUnderTheRailScrollsBackToTheRailLine() {
        assertEquals(-80f, insetScrollDistance(offset = 36f, size = 130f, containerSize = 960f, leading = 116f, trailing = 48f))
    }

    @Test
    fun aCardInsideTheInsetsDoesNotScroll() {
        assertEquals(0f, insetScrollDistance(offset = 266f, size = 130f, containerSize = 960f, leading = 116f, trailing = 48f))
    }

    @Test
    fun aCardPastTheRightOverscanScrollsByItsOverflow() {
        assertEquals(84f, insetScrollDistance(offset = 866f, size = 130f, containerSize = 960f, leading = 116f, trailing = 48f))
    }

    @Test
    fun aChildLargerThanTheSpaceAlignsToTheLeadingInset() {
        assertEquals(84f, insetScrollDistance(offset = 200f, size = 900f, containerSize = 960f, leading = 116f, trailing = 48f))
    }
}
