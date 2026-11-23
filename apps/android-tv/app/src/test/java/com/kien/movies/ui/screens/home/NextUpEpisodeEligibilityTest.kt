package com.kien.movies.ui.screens.home

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NextUpEpisodeEligibilityTest {

    private val today: LocalDate = LocalDate.of(2026, 8, 19)

    @Test
    fun `a past date is aired`() {
        assertFalse(isNextUpEpisodeUnaired(today.minusDays(1), today))
    }

    @Test
    fun `an episode airing today is aired`() {
        assertFalse(isNextUpEpisodeUnaired(today, today))
    }

    @Test
    fun `a future date is unaired`() {
        assertTrue(isNextUpEpisodeUnaired(today.plusDays(1), today))
    }

    @Test
    fun `a missing date is unaired rather than aired`() {
        assertTrue(isNextUpEpisodeUnaired(null, today))
    }
}
