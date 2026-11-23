package com.kien.movies.ui.screens.player

import androidx.media3.exoplayer.SeekParameters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * mpv lists the chapters itself; they come with the file, so once read for a stream they are kept.
 * They always feed skip intro, and reach the UI only while the chapters setting is on, the same as
 * on ExoPlayer. Turning the setting on mid-stream reads them again on the next tick.
 */
internal fun PlayerRuntimeController.refreshMpvChapters(view: MoviesMpvSurfaceView) {
    val showChapters = _uiState.value.chaptersEnabled
    if (mpvChaptersStreamUrl == currentStreamUrl &&
        chapterSkipDetection.hasChapters &&
        (!showChapters || _uiState.value.chapters.isNotEmpty())
    ) {
        return
    }
    val chapters = PlayerChapters.normalize(view.readChapters())
    if (chapters.isEmpty()) return
    mpvChaptersStreamUrl = currentStreamUrl
    if (showChapters) _uiState.update { it.copy(chapters = chapters) }
    onChaptersLoaded(chapters)
}

/**
 * Exact on both engines: a keyframe seek can land before the chapter start, and "next" would then
 * keep returning to the same chapter. On mpv, setting the position seeks to a keyframe when the
 * file was loaded with `hr-seek=no` (the Hi10P software fallback), so chapters ask for an exact seek.
 */
internal fun PlayerRuntimeController.seekToChapter(startMs: Long) {
    if (_playbackTimeline.value.isLive) return
    pendingPreviewSeekPosition = null
    seekPlaybackTo(startMs, SeekParameters.EXACT, exact = true)
    updatePlaybackTimeline(currentPosition = startMs)
    scheduleProgressSyncAfterSeek()
    if (_uiState.value.showControls) {
        showControlsTemporarily()
    } else {
        showSeekOverlayTemporarily()
    }
}

internal fun PlayerRuntimeController.showChaptersPanel() {
    if (_uiState.value.chapters.isEmpty()) return
    hideControlsJob?.cancel()
    _uiState.update {
        it.copy(
            showChaptersPanel = true,
            showControls = true,
            showAudioOverlay = false,
            showSubtitleOverlay = false,
            showSubtitleStylePanel = false,
            showSubtitleTimingDialog = false,
            showSpeedDialog = false,
            showMoreDialog = false
        )
    }
}

internal fun PlayerRuntimeController.dismissChaptersPanel() {
    _uiState.update { it.copy(showChaptersPanel = false) }
    scheduleHideControls()
}

/**
 * ExoPlayer does not read Matroska chapters, so they are read from the file once its first frame
 * is shown. Chapters are loaded when the chapters UI is enabled OR skip intro is enabled,
 * and only for Matroska streams (the loader returns empty for non-MKV).
 */
internal fun PlayerRuntimeController.loadExoChapters() {
    exoChapterLoadJob?.cancel()
    if (isUsingMpvEngine() || _playbackTimeline.value.isLive) return
    val url = currentStreamUrl
    val headers = currentHeaders
    exoChapterLoadJob = scope.launch {
        val settings = playerSettingsDataStore.playerSettings.first()
        if (!settings.exoChaptersEnabled && !skipIntroEnabled) return@launch
        // Only probe for chapters if the container is Matroska/WebM.
        // This avoids the extra HTTP request for MP4, HLS, and other formats.
        val containerMime = _exoPlayer?.videoFormat?.containerMimeType
        if (containerMime != null &&
            containerMime != "video/x-matroska" &&
            containerMime != "video/webm"
        ) {
            return@launch
        }
        val chapters = MatroskaChapterLoader.load(url, headers)
        if (chapters.isEmpty() || isUsingMpvEngine() || currentStreamUrl != url) return@launch
        if (settings.exoChaptersEnabled) {
            _uiState.update { it.copy(chapters = chapters) }
        }
        onChaptersLoaded(chapters)
    }
}

private fun PlayerRuntimeController.onChaptersLoaded(chapters: List<PlayerChapter>) {
    chapterSkipDetection.load(chapters)
    if (skipIntroEnabled) {
        tryComputeChapterSkipIntervals()
    }
}

private fun PlayerRuntimeController.tryComputeChapterSkipIntervals() {
    if (chapterSkipDetection.computeIntervals(_playbackTimeline.value.duration)) {
        recomputeMergedSkipIntervals()
    }
}

internal fun PlayerRuntimeController.recomputeChapterSkipIntervalsIfNeeded() {
    if (!skipIntroEnabled) return
    if (chapterSkipDetection.isComputed) return
    tryComputeChapterSkipIntervals()
}
