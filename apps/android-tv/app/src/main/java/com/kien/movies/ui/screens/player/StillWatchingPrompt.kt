package com.kien.movies.ui.screens.player

import com.kien.movies.data.local.PlayerSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

private const val STILL_WATCHING_COUNTDOWN_SECONDS = 60
private const val STILL_WATCHING_COUNTDOWN_TICK_MS = 1_000L

internal fun shouldEnterStillWatchingPrompt(
    stillWatchingEnabled: Boolean,
    autoPlayNextEpisodeEnabled: Boolean,
    nextEpisodeHasAired: Boolean,
    consecutiveAutoPlayCount: Int,
    threshold: Int
): Boolean = stillWatchingEnabled &&
    autoPlayNextEpisodeEnabled &&
    nextEpisodeHasAired &&
    consecutiveAutoPlayCount >= threshold

internal class StillWatchingPrompt(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<PlayerUiState>,
    private val pausePlayback: () -> Unit,
    private val playNextEpisode: () -> Unit,
    private val resetPostPlayOverlay: () -> Unit
) {
    var enabled: Boolean = false
    var episodeThreshold: Int = PlayerSettings.DEFAULT_STILL_WATCHING_EPISODE_THRESHOLD
    private var consecutiveAutoPlayCount: Int = 0
    private var countdownJob: Job? = null

    fun recordEpisodeSwitch(isAutoPlay: Boolean) {
        consecutiveAutoPlayCount = nextConsecutiveAutoPlayCount(
            currentCount = consecutiveAutoPlayCount,
            isAutoPlay = isAutoPlay
        )
    }

    fun isDue(autoPlayNextEpisodeEnabled: Boolean, nextEpisodeHasAired: Boolean): Boolean {
        return shouldEnterStillWatchingPrompt(
            stillWatchingEnabled = enabled,
            autoPlayNextEpisodeEnabled = autoPlayNextEpisodeEnabled,
            nextEpisodeHasAired = nextEpisodeHasAired,
            consecutiveAutoPlayCount = consecutiveAutoPlayCount,
            threshold = episodeThreshold
        )
    }

    fun show() {
        val nextInfo = uiState.value.nextEpisode ?: return
        if (uiState.value.postPlayMode is PostPlayMode.StillWatching &&
            countdownJob?.isActive == true
        ) {
            return
        }
        pausePlayback()
        countdownJob?.cancel()
        uiState.update {
            it.copy(
                postPlayMode = PostPlayMode.StillWatching(
                    nextEpisode = nextInfo,
                    countdownSec = STILL_WATCHING_COUNTDOWN_SECONDS
                ),
                showControls = false
            )
        }
        val job = scope.launch {
            try {
                for (remaining in (STILL_WATCHING_COUNTDOWN_SECONDS - 1) downTo 0) {
                    delay(STILL_WATCHING_COUNTDOWN_TICK_MS)
                    uiState.update { state ->
                        val current = state.postPlayMode as? PostPlayMode.StillWatching
                            ?: return@update state
                        state.copy(postPlayMode = current.copy(countdownSec = remaining))
                    }
                }
                if (uiState.value.postPlayMode is PostPlayMode.StillWatching) {
                    dismiss()
                }
            } finally {
                if (countdownJob === coroutineContext.job) {
                    countdownJob = null
                }
            }
        }
        countdownJob = job
    }

    fun continueWatching() {
        cancel()
        consecutiveAutoPlayCount = 0
        uiState.update { it.copy(postPlayMode = null) }
        playNextEpisode()
    }

    fun dismiss() {
        consecutiveAutoPlayCount = 0
        resetPostPlayOverlay()
        uiState.update { it.copy(pendingExitReason = PlayerExitReason.StillWatchingPrompt) }
    }

    fun cancel() {
        countdownJob?.cancel()
        countdownJob = null
    }
}
