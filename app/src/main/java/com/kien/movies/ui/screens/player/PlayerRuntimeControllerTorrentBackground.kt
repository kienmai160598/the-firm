package com.kien.movies.ui.screens.player

import android.util.Log
import com.kien.movies.domain.model.Stream
import kotlinx.coroutines.flow.update

private const val TAG = "PlayerTorrent"

// What it takes to start the same torrent again. stopTorrentStream() clears these fields, so this
// is read before the player or the stream is released.
internal data class TorrentStreamIdentity(
    val infoHash: String,
    val fileIdx: Int?,
    val sources: List<String>?
)

internal data class TorrentBackgroundResume(
    val identity: TorrentStreamIdentity,
    val positionMs: Long
)

private fun PlayerRuntimeController.currentTorrentStreamIdentity(): TorrentStreamIdentity? {
    val infoHash = currentInfoHash?.takeIf { isTorrentStream } ?: return null

    return TorrentStreamIdentity(infoHash, currentFileIdx, currentTorrentSources)
}

private fun PlayerRuntimeController.restartTorrentPaused(identity: TorrentStreamIdentity) {
    launchTorrentSourceStream(
        stream = Stream(
            name = _uiState.value.currentStreamName,
            title = null,
            description = null,
            url = null,
            ytId = null,
            infoHash = identity.infoHash,
            fileIdx = identity.fileIdx,
            externalUrl = null,
            behaviorHints = null,
            addonName = currentAddonName ?: "",
            addonLogo = currentAddonLogo,
            sources = identity.sources
        ),
        infoHash = identity.infoHash,
        loadSavedProgress = false,
        startPaused = true
    )
}

// A decoder crash in the background has already released the player and kept the position it had.
// A seek the player has not applied yet (a restart still loading) is where playback really is.
internal fun torrentBackgroundResumePositionMs(
    crashRecoveryPending: Boolean,
    crashSavedPositionMs: Long,
    playerPositionMs: Long?,
    pendingSeekPositionMs: Long? = null
): Long {
    val pendingSeek = pendingSeekPositionMs?.takeIf { it > 0L }
    val positionMs = when {
        crashRecoveryPending -> crashSavedPositionMs
        pendingSeek != null -> pendingSeek
        else -> playerPositionMs ?: 0L
    }

    return positionMs.coerceAtLeast(0L)
}

// With the app in the background a torrent would keep downloading and seeding, on a device where
// the foreground app needs the memory and the network. The engine is shut down, and the same
// torrent starts again, paused at the same position, when the player is back on screen.
internal fun PlayerRuntimeController.releaseTorrentForBackground(): Boolean {
    if (torrentBackgroundResume != null) return true
    val identity = currentTorrentStreamIdentity() ?: return false
    val positionMs = torrentBackgroundResumePositionMs(
        crashRecoveryPending = pendingBackgroundCrashRecovery,
        crashSavedPositionMs = backgroundCrashSavedPositionMs,
        playerPositionMs = currentPlaybackPositionMs(),
        pendingSeekPositionMs = _uiState.value.pendingSeekPosition
    )

    saveWatchProgress()
    emitPauseScrobbleForCurrentProgress()
    errorRetryJob?.cancel()
    playbackPreparationJob?.cancel()
    torrentBackgroundResume = TorrentBackgroundResume(identity, positionMs)

    releasePlayer(flushPlaybackState = false)
    stopTorrentStream()
    torrentService.shutdown()
    Log.i(TAG, "Torrent stopped for background at ${positionMs}ms")
    return true
}

internal fun PlayerRuntimeController.resumeTorrentAfterBackground(): Boolean {
    val resume = torrentBackgroundResume ?: return false
    torrentBackgroundResume = null
    pendingBackgroundCrashRecovery = false
    backgroundCrashSavedPositionMs = 0L
    resetErrorRetryState()

    _uiState.update { state ->
        state.copy(
            error = null,
            showLoadingOverlay = state.loadingOverlayEnabled,
            pendingSeekPosition = resume.positionMs.takeIf { it > 0L } ?: state.pendingSeekPosition
        )
    }
    Log.i(TAG, "Torrent restarting after background at ${resume.positionMs}ms")
    restartTorrentPaused(resume.identity)
    return true
}
