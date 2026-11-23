package com.kien.movies.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStateTest {
    private val playing = PlayerState().on(PlayerEvent.Started).on(PlayerEvent.Engine(isBuffering = false, playWhenReady = true, isPlaying = true))

    @Test
    fun engineReportsBeforeTheStartKeepLoading() {
        val state = PlayerState().on(PlayerEvent.Engine(isBuffering = false, playWhenReady = true, isPlaying = true))

        assertEquals(PlaybackPhase.Loading, state.phase)
        assertFalse(state.hasPlayed)
    }

    @Test
    fun bufferingBeforeTheFirstFrameIsStillLoading() {
        val state = PlayerState().on(PlayerEvent.Started).on(PlayerEvent.Engine(isBuffering = true, playWhenReady = true, isPlaying = false))

        assertEquals(PlaybackPhase.Loading, state.phase)
    }

    @Test
    fun bufferingAfterPlayingIsBuffering() {
        val state = playing.on(PlayerEvent.Engine(isBuffering = true, playWhenReady = true, isPlaying = false))

        assertEquals(PlaybackPhase.Buffering, state.phase)
        assertTrue(state.hasPlayed)
    }

    @Test
    fun pausingAfterPlayingShowsThePauseIcon() {
        val state = playing.on(PlayerEvent.Engine(isBuffering = false, playWhenReady = false, isPlaying = false))

        assertEquals(PlaybackPhase.Paused, state.phase)
        assertTrue(state.isPausedByViewer)
    }

    @Test
    fun endNearTheDurationEnds() {
        val state = playing.on(PlayerEvent.Ended(positionMs = 96_000, durationMs = 100_000))

        assertEquals(PlaybackPhase.Ended, state.phase)
    }

    @Test
    fun endFarFromTheDurationFailsAsStoppedEarly() {
        val state = playing.on(PlayerEvent.Ended(positionMs = 40_000, durationMs = 100_000))

        assertEquals(PlaybackPhase.Failed, state.phase)
        assertEquals(PlayerState.STOPPED_EARLY, state.failure)
    }

    @Test
    fun aFailureIsFinal() {
        val failed = playing.on(PlayerEvent.Failed("Source error"))

        val state = failed.on(PlayerEvent.Engine(isBuffering = false, playWhenReady = true, isPlaying = true))

        assertEquals(PlaybackPhase.Failed, state.phase)
        assertEquals("Source error", state.failure)
    }
}
