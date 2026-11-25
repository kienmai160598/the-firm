package com.nuvio.tvos.rules

import com.nuvio.app.features.watchprogress.WatchProgressEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResumeStartTest {
    private val streamLengthMs = 7_200_000L

    @Test
    fun aSavedPositionWithItsLengthStartsWhereTheResumeRuleLandsAfterLoading() {
        val saved = progress(positionMs = 1_500_000, durationMs = 7_000_000)

        val start = ResumeStart.positionBeforeLoadMs(saved.lastPositionMs, saved.durationMs)

        assertEquals(1_500_000L, start)
        assertEquals(saved.resolveResumePosition(streamLengthMs), start)
    }

    @Test
    fun progressWithoutALengthWaitsForTheStream() {
        val start = ResumeStart.positionBeforeLoadMs(savedPositionMs = 1_500_000, savedDurationMs = 0)

        assertNull(start)
    }

    private fun progress(positionMs: Long, durationMs: Long) = WatchProgressEntry(
        contentType = "movie",
        parentMetaId = "tt1",
        parentMetaType = "movie",
        videoId = "tt1",
        title = "Film",
        lastPositionMs = positionMs,
        durationMs = durationMs,
        lastUpdatedEpochMs = 0,
    )
}
