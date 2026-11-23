package com.nuvio.tvos.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Ported from apps/apple-tv/Tests/PlaybackAdvisoryTests.swift.
class PlaybackAdvisoryTest {
    @Test
    fun showsTheTrimmedRatingAndUpToThreeGenres() {
        val advisory = PlaybackAdvisory.of(
            ageRating = " PG-13 ",
            genres = listOf("Action", " ", "Drama", "Thriller", "Crime"),
        )

        assertEquals("PG-13", advisory?.ageRating)
        assertEquals("Action, Drama, Thriller", advisory?.genreLine)
    }

    @Test
    fun genresAloneWhenThereIsNoRating() {
        val advisory = PlaybackAdvisory.of(ageRating = "", genres = listOf("Comedy"))

        assertNull(advisory?.ageRating)
        assertEquals("Comedy", advisory?.genreLine)
    }

    @Test
    fun nothingToShowWithoutRatingOrGenres() {
        assertNull(PlaybackAdvisory.of(ageRating = null, genres = emptyList()))
    }
}
