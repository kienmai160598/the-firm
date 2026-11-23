package com.kien.movies.ui.screens.player

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StillWatchingPromptTest {

    private val nextEpisode = NextEpisodeInfo(
        videoId = "v2",
        season = 1,
        episode = 2,
        title = "Ep",
        thumbnail = null,
        overview = null,
        released = null,
        hasAired = true,
        unairedMessage = null
    )
    private val testScope = TestScope()
    private val uiState = MutableStateFlow(PlayerUiState(nextEpisode = nextEpisode))
    private var pauseCount = 0
    private var playNextCount = 0
    private var resetOverlayCount = 0
    private val prompt = StillWatchingPrompt(
        scope = testScope,
        uiState = uiState,
        pausePlayback = { pauseCount += 1 },
        playNextEpisode = { playNextCount += 1 },
        resetPostPlayOverlay = { resetOverlayCount += 1 }
    ).apply {
        enabled = true
        episodeThreshold = 2
    }

    @Test
    fun `prompt is due after the threshold of consecutive autoplays`() {
        prompt.recordEpisodeSwitch(isAutoPlay = true)
        val dueAfterOne = isPromptDue()
        prompt.recordEpisodeSwitch(isAutoPlay = true)

        assertFalse(dueAfterOne)
        assertTrue(isPromptDue())
    }

    @Test
    fun `a manual episode switch starts the count again`() {
        prompt.recordEpisodeSwitch(isAutoPlay = true)
        prompt.recordEpisodeSwitch(isAutoPlay = true)

        prompt.recordEpisodeSwitch(isAutoPlay = false)

        assertFalse(isPromptDue())
    }

    @Test
    fun `unanswered prompt exits the player when the countdown ends`() {
        prompt.show()
        testScope.runCurrent()

        testScope.advanceTimeBy(59_000L)
        testScope.runCurrent()
        val countdownBeforeEnd = (uiState.value.postPlayMode as PostPlayMode.StillWatching)
            .countdownSec
        testScope.advanceTimeBy(1_000L)
        testScope.runCurrent()

        assertEquals(1, pauseCount)
        assertEquals(1, countdownBeforeEnd)
        assertEquals(1, resetOverlayCount)
        assertEquals(PlayerExitReason.StillWatchingPrompt, uiState.value.pendingExitReason)
    }

    @Test
    fun `continue plays the next episode and clears the count`() {
        prompt.recordEpisodeSwitch(isAutoPlay = true)
        prompt.recordEpisodeSwitch(isAutoPlay = true)
        prompt.show()

        prompt.continueWatching()
        testScope.advanceTimeBy(60_000L)
        testScope.runCurrent()

        assertEquals(1, playNextCount)
        assertNull(uiState.value.postPlayMode)
        assertNull(uiState.value.pendingExitReason)
        assertFalse(isPromptDue())
    }

    @Test
    fun `cancel stops the countdown`() {
        prompt.show()

        prompt.cancel()
        testScope.advanceTimeBy(60_000L)
        testScope.runCurrent()

        assertEquals(0, resetOverlayCount)
        assertNull(uiState.value.pendingExitReason)
    }

    private fun isPromptDue(): Boolean {
        return prompt.isDue(autoPlayNextEpisodeEnabled = true, nextEpisodeHasAired = true)
    }
}
