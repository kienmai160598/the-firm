package com.kien.movies.ui.screens.player

import android.net.Uri
import androidx.media3.common.Player
import com.kien.movies.data.repository.PlaybackIssueLoadingEventInput
import com.kien.movies.data.repository.PlaybackIssueLoadingInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOADING_ISSUE_REPORT_DELAY_MS = 45_000L

internal fun PlayerRuntimeController.resetLoadingDiagnostics(
    phase: String,
    message: String? = null,
    progress: Float? = null
) {
    loadingDiagnostics.reset(phase = phase, message = message, progress = progress)
    recordLoadingDiagnosticEvent(phase = phase, message = message, progress = progress)
    scheduleLoadingIssueReportAvailability()
}

internal fun PlayerRuntimeController.setLoadingStatus(
    phase: String,
    message: String?,
    progress: Float? = null,
    showOverlay: Boolean? = null,
    detail: String? = null
) {
    recordLoadingDiagnosticEvent(
        phase = phase,
        message = message,
        progress = progress,
        detail = detail
    )
    _uiState.update { state ->
        state.copy(
            showLoadingOverlay = showOverlay ?: state.showLoadingOverlay,
            loadingMessage = message,
            loadingProgress = progress
        )
    }
}

internal fun PlayerRuntimeController.recordLoadingDiagnosticEvent(
    phase: String,
    message: String? = null,
    progress: Float? = null,
    detail: String? = null
) {
    loadingDiagnostics.record(
        phase = phase,
        message = message,
        progress = progress,
        detail = detail,
        launchStartedAtMs = launchStartedAtElapsedMs,
        describePlayback = {
            "engine=$currentInternalPlayerEngine host=${currentStreamUrl.safePlaybackRawHost()}"
        }
    )
}

internal fun PlayerRuntimeController.finishLoadingDiagnostics(phase: String) {
    recordLoadingDiagnosticEvent(phase = phase, message = null, progress = 1f)
    startupLoadingReportJob?.cancel()
    startupLoadingReportJob = null
    _uiState.update {
        it.copy(
            loadingIssueReportVisible = false,
            loadingIssueElapsedMs = 0L
        )
    }
}

internal fun PlayerRuntimeController.scheduleLoadingIssueReportAvailability() {
    startupLoadingReportJob?.cancel()
    startupLoadingReportJob = scope.launch {
        delay(LOADING_ISSUE_REPORT_DELAY_MS)
        val state = _uiState.value
        if (state.playbackIssueReportsEnabled && !hasRenderedFirstFrame && state.error == null &&
            state.showLoadingOverlay
        ) {
            val elapsedMs = loadingDiagnostics.elapsedSinceStartMs()
            _uiState.update {
                it.copy(
                    loadingIssueReportVisible = true,
                    loadingIssueElapsedMs = elapsedMs
                )
            }
        }
    }
}

internal fun PlayerRuntimeController.buildPlaybackIssueLoadingInput(
    reportReason: String
): PlaybackIssueLoadingInput {
    val state = _uiState.value
    val player = _exoPlayer
    return PlaybackIssueLoadingInput(
        phase = loadingDiagnostics.phase.ifBlank { "unknown" },
        message = state.loadingMessage ?: loadingDiagnostics.lastMessage,
        progress = state.loadingProgress ?: loadingDiagnostics.lastProgress,
        elapsedMs = loadingDiagnostics.elapsedForReportMs(),
        phaseElapsedMs = loadingDiagnostics.phaseElapsedForReportMs(),
        reportReason = reportReason,
        loadingOverlayVisible = state.showLoadingOverlay,
        loadingStatusVisible = state.showPlayerLoadingStatus,
        hasRenderedFirstFrame = hasRenderedFirstFrame,
        exoPlayerCreated = player != null,
        exoPlaybackState = player?.playbackState,
        exoPlaybackStateName = player?.playbackState?.playbackStateName(),
        exoIsLoading = player?.isLoading,
        exoPlayWhenReady = player?.playWhenReady,
        mpvAttached = mpvView != null,
        startupRetryCount = startupRetryCount,
        errorRetryCount = errorRetryCount,
        timeoutRecoveryAttempts = timeoutRecoveryAttempts,
        isLoadingAddonSubtitles = state.isLoadingAddonSubtitles,
        addonSubtitlesCount = state.addonSubtitles.size,
        isLoadingSourceStreams = state.isLoadingSourceStreams,
        isLoadingEpisodeStreams = state.isLoadingEpisodeStreams,
        torrentDownloadSpeed = state.torrentDownloadSpeed,
        torrentPeers = state.torrentPeers,
        torrentSeeds = state.torrentSeeds,
        rawEventLines = loadingDiagnostics.recordedRawLines,
        events = loadingDiagnostics.recordedEvents.map {
            PlaybackIssueLoadingEventInput(
                timeMs = it.timeMs,
                elapsedMs = it.elapsedMs,
                phase = it.phase,
                message = it.message,
                progress = it.progress,
                detail = it.detail
            )
        }
    )
}

private fun Int.playbackStateName(): String = when (this) {
    Player.STATE_IDLE -> "IDLE"
    Player.STATE_BUFFERING -> "BUFFERING"
    Player.STATE_READY -> "READY"
    Player.STATE_ENDED -> "ENDED"
    else -> "UNKNOWN"
}

private fun String.safePlaybackRawHost(): String {
    return runCatching {
        Uri.parse(this).host ?: substringBefore("://").takeIf { it.isNotBlank() } ?: "unknown"
    }.getOrDefault("unknown")
}
