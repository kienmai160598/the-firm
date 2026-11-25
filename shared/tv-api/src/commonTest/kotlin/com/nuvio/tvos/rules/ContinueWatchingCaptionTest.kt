package com.nuvio.tvos.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class ContinueWatchingCaptionTest {
    @Test
    fun anEpisodeShowsItsCodeAndTheTimeLeft() {
        val caption = ContinueWatchingCaption.of(1, 1, ContinueWatchingCaption.timeLeft(0, 17))

        assertEquals("S1, E1 · 17m", caption)
    }

    @Test
    fun aMovieShowsOnlyTheTimeLeft() {
        val caption = ContinueWatchingCaption.of(null, null, ContinueWatchingCaption.timeLeft(1, 11))

        assertEquals("1h 11m", caption)
    }

    @Test
    fun anUnstartedEpisodeShowsItsCodeAndState() {
        val caption = ContinueWatchingCaption.of(2, 1, "Airs Today")

        assertEquals("S2, E1 · Airs Today", caption)
    }
}
