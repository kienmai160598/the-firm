package com.kien.movies.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLoadingDiagnosticsLogTest {

    private var elapsedRealtimeMs = 1_000L
    private val log = PlayerLoadingDiagnosticsLog(
        elapsedRealtimeMs = { elapsedRealtimeMs },
        wallClockMs = { 5_000L }
    )

    @Test
    fun `repeating the same status records one event`() {
        log.reset(phase = "resolve", message = null, progress = null)

        record(phase = "resolve", message = "Resolving", progress = 0.5f)
        record(phase = "resolve", message = "Resolving", progress = 0.5f)

        assertEquals(1, log.recordedEvents.size)
        assertEquals(1, log.recordedRawLines.size)
    }

    @Test
    fun `events keep the newest 80 and raw lines the newest 120`() {
        log.reset(phase = "start", message = null, progress = null)

        repeat(130) { index -> record(phase = "phase$index") }

        assertEquals(80, log.recordedEvents.size)
        assertEquals("phase50", log.recordedEvents.first().phase)
        assertEquals(120, log.recordedRawLines.size)
        assertTrue(log.recordedRawLines.first().contains("seq=11 phase=phase10 "))
    }

    @Test
    fun `elapsed times count from the reset and from the phase change`() {
        log.reset(phase = "resolve", message = null, progress = null)
        elapsedRealtimeMs = 1_400L
        record(phase = "buffer")
        elapsedRealtimeMs = 1_900L

        record(phase = "buffer", message = "Buffering")

        assertEquals(400L, log.recordedEvents.first().elapsedMs)
        assertEquals(900L, log.elapsedForReportMs())
        assertEquals(500L, log.phaseElapsedForReportMs())
        assertTrue(log.recordedRawLines.last().contains("elapsedMs=900 phaseElapsedMs=500 "))
    }

    private fun record(phase: String, message: String? = null, progress: Float? = null) {
        log.record(
            phase = phase,
            message = message,
            progress = progress,
            detail = null,
            launchStartedAtMs = null,
            describePlayback = { "engine=EXOPLAYER host=example.org" }
        )
    }
}
