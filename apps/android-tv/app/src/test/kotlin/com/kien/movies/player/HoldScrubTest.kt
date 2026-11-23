package com.kien.movies.player

import org.junit.Assert.assertEquals
import org.junit.Test

class HoldScrubTest {
    private val movieSeconds = 7_200

    @Test
    fun speedRisesWithHeldTime() {
        val speeds = listOf(500L, 2_000L, 4_000L).map { HoldScrub.speed(heldMillis = it, durationSeconds = movieSeconds) }

        assertEquals(listOf(20.0, 60.0, 180.0), speeds)
    }

    @Test
    fun aShortVideoTakesAtLeastTenHeldSecondsToCross() {
        val speed = HoldScrub.speed(heldMillis = 4_000, durationSeconds = 300)

        assertEquals(30.0, speed, 0.0)
    }

    @Test
    fun oneRepeatAdvancesBySpeedTimesTheGapSinceTheLastKey() {
        val advance = HoldScrub.advanceSeconds(heldMillis = 2_000, sinceLastKeyMillis = 50, durationSeconds = movieSeconds)

        assertEquals(3.0, advance, 1e-9)
    }

    @Test
    fun thePreviewStaysInsideTheVideo() {
        val session = ScrubSession(originSeconds = 590, durationSeconds = 600)

        val moved = session.moved(bySeconds = 30.0)

        assertEquals(600, moved.shownSeconds)
    }

    @Test
    fun theBadgeAddsUpInOneDirectionAndRestartsOnTheOther() {
        val twoForward = SkipBadge(isForward = true, presses = 1).adding(isForward = true)

        val back = twoForward.adding(isForward = false)

        assertEquals(20, twoForward.seconds)
        assertEquals(SkipBadge(isForward = false, presses = 1), back)
    }
}
