package com.nuvio.tvos.rules

import com.nuvio.tvos.rules.ProgressSavePolicy.Moment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressSavePolicyTest {
    @Test
    fun nothingIsSavedBeforePlaybackHasStarted() {
        val saves = Moment.entries.map { moment ->
            ProgressSavePolicy.shouldSave(moment, hasPlayed = false, positionMs = 5_000, lastTickPositionMs = null)
        }

        assertEquals(List(Moment.entries.size) { false }, saves)
    }

    @Test
    fun aTickSkipsWhenThePositionMovedLessThanOneSecond() {
        val save = ProgressSavePolicy.shouldSave(Moment.Tick, hasPlayed = true, positionMs = 60_900, lastTickPositionMs = 60_000)

        assertFalse(save)
    }

    @Test
    fun aTickSavesOnceThePositionMovedOneSecondEitherWay() {
        val forward = ProgressSavePolicy.shouldSave(Moment.Tick, hasPlayed = true, positionMs = 61_000, lastTickPositionMs = 60_000)
        val back = ProgressSavePolicy.shouldSave(Moment.Tick, hasPlayed = true, positionMs = 59_000, lastTickPositionMs = 60_000)

        assertTrue(forward && back)
    }

    @Test
    fun pauseSavesEvenWhenThePositionDidNotMove() {
        val save = ProgressSavePolicy.shouldSave(Moment.Pause, hasPlayed = true, positionMs = 60_000, lastTickPositionMs = 60_000)

        assertTrue(save)
    }

    @Test
    fun onlyTheTickStaysLocal() {
        val syncing = Moment.entries.filter(Moment::syncsRemote)

        assertEquals(listOf(Moment.Pause, Moment.End, Moment.Exit, Moment.Background), syncing)
    }

    @Test
    fun anEndedPlaybackStoresTheFullDuration() {
        val position = ProgressSavePolicy.savedPositionMs(isEnded = true, positionMs = 5_990_000, durationMs = 6_000_000)

        assertEquals(6_000_000, position)
    }
}
