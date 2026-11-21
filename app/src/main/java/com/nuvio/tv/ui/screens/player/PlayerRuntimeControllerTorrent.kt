package com.nuvio.tv.ui.screens.player

import android.util.Log
import com.nuvio.tv.R
import com.nuvio.tv.core.torrent.TorrentState
import com.nuvio.tv.core.torrent.torrentInitialLoadingProgress
import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PlayerTorrent"

internal suspend fun PlayerRuntimeController.startTorrentStream(
    infoHash: String,
    fileIdx: Int?,
    filename: String? = null,
    trackers: List<String> = emptyList()
): String {
    isTorrentStream = true
    currentInfoHash = infoHash
    currentFileIdx = fileIdx

    setLoadingStatus(
        phase = "torrent_starting_engine",
        message = context.getString(R.string.player_torrent_starting_engine),
        showOverlay = true
    )
    _uiState.update {
        it.copy(
            showLoadingOverlay = true,
            loadingMessage = context.getString(R.string.player_torrent_starting_engine),
            loadingProgress = null,
            isTorrentStream = true
        )
    }

    val effectiveFilename = filename ?: currentFilename
    return torrentService.startStream(infoHash, fileIdx, effectiveFilename, trackers)
}

internal fun PlayerRuntimeController.stopTorrentStream() {
    torrentStreamJob?.cancel()
    torrentStreamJob = null
    torrentStateObserverJob?.cancel()
    torrentStateObserverJob = null

    if (isTorrentStream) {
        torrentService.stopStream()
    }

    isTorrentStream = false
    currentInfoHash = null
    currentFileIdx = null
}

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

internal fun PlayerRuntimeController.currentTorrentStreamIdentity(): TorrentStreamIdentity? {
    val infoHash = currentInfoHash?.takeIf { isTorrentStream } ?: return null

    return TorrentStreamIdentity(infoHash, currentFileIdx, currentTorrentSources)
}

internal fun PlayerRuntimeController.relaunchTorrentStream(
    identity: TorrentStreamIdentity,
    loadSavedProgress: Boolean,
    startPaused: Boolean = false
) {
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
        loadSavedProgress = loadSavedProgress,
        startPaused = startPaused
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
    relaunchTorrentStream(resume.identity, loadSavedProgress = false, startPaused = true)
    return true
}

internal fun PlayerRuntimeController.observeTorrentState() {
    torrentStateObserverJob?.cancel()
    torrentStateObserverJob = scope.launch {
        torrentService.state.collectLatest { torrentState ->
            when (torrentState) {
                is TorrentState.Idle -> Unit
                is TorrentState.Connecting -> onTorrentConnecting(torrentState)
                is TorrentState.Streaming -> onTorrentStreaming(torrentState)
                is TorrentState.Error -> {
                    Log.e(TAG, "Torrent error: ${torrentState.message}")
                    _uiState.update {
                        it.copy(
                            error = context.getString(R.string.player_error_torrent, torrentState.message),
                            showLoadingOverlay = false,
                            torrentBufferingMessage = null
                        )
                    }
                }
            }
        }
    }
}

private fun PlayerRuntimeController.onTorrentConnecting(torrentState: TorrentState.Connecting) {
    if (hasRenderedFirstFrame) return
    val phaseLabel = context.getString(
        when (torrentState.phase) {
            "add_magnet" -> R.string.player_torrent_fetching_metadata
            "prepare_stream", "make_room", "attach_route" -> R.string.player_torrent_preparing_stream
            else -> R.string.player_torrent_starting_engine
        }
    )
    val message = if (_uiState.value.hideTorrentStats) {
        phaseLabel
    } else {
        context.getString(
            R.string.player_torrent_connecting_status,
            phaseLabel,
            context.getString(R.string.player_torrent_peer_info, torrentState.seeds, torrentState.peers),
            formatSpeed(context, torrentState.downloadSpeed)
        )
    }
    recordLoadingDiagnosticEvent(
        phase = "torrent_connecting_peers",
        message = message
    )
    _uiState.update {
        it.copy(
            showLoadingOverlay = true,
            loadingMessage = message,
            loadingProgress = null,
            torrentBufferingMessage = null
        )
    }
}

private fun PlayerRuntimeController.onTorrentStreaming(torrentState: TorrentState.Streaming) {
    val speed = formatSpeed(context, torrentState.downloadSpeed)
    val peerInfo = context.getString(R.string.player_torrent_peer_info, torrentState.seeds, torrentState.peers)
    val statsHidden = _uiState.value.hideTorrentStats

    if (!hasRenderedFirstFrame) {
        val bufferedAheadMs = _exoPlayer
            ?.let { player -> (player.bufferedPosition - player.currentPosition).coerceAtLeast(0L) }
            ?: 0L
        val progress = torrentInitialLoadingProgress(
            bufferedAheadMs = bufferedAheadMs,
            downloadedBytes = torrentState.downloadedBytes,
            deliveredBytes = torrentState.deliveredBytes
        )
        val message = if (statsHidden) {
            null
        } else {
            context.getString(
                R.string.player_torrent_loading_status,
                formatMB(context, torrentState.loadedBytes),
                peerInfo,
                speed
            )
        }
        recordLoadingDiagnosticEvent(
            phase = "torrent_preloading",
            message = message,
            progress = progress,
            detail = "${torrentState.seeds}/${torrentState.peers}"
        )
        _uiState.update {
            it.copy(
                showLoadingOverlay = true,
                loadingMessage = message,
                loadingProgress = progress,
                torrentDownloadSpeed = torrentState.downloadSpeed,
                torrentUploadSpeed = torrentState.uploadSpeed,
                torrentPeers = torrentState.peers,
                torrentSeeds = torrentState.seeds,
                torrentBufferProgress = torrentState.bufferProgress,
                torrentTotalProgress = torrentState.totalProgress,
                torrentBufferingMessage = null
            )
        }
    } else {
        val message = if (statsHidden) null else context.getString(R.string.player_torrent_status, peerInfo, speed)
        _uiState.update {
            it.copy(
                loadingProgress = null,
                torrentDownloadSpeed = torrentState.downloadSpeed,
                torrentUploadSpeed = torrentState.uploadSpeed,
                torrentPeers = torrentState.peers,
                torrentSeeds = torrentState.seeds,
                torrentBufferProgress = torrentState.bufferProgress,
                torrentTotalProgress = torrentState.totalProgress,
                torrentBufferingMessage = message
            )
        }
    }
}

internal fun PlayerRuntimeController.launchTorrentSourceStream(
    stream: Stream,
    infoHash: String,
    loadSavedProgress: Boolean,
    startPaused: Boolean = false
) {
    torrentStreamJob?.cancel()
    torrentStreamJob = scope.launch {
        try {
            observeTorrentState()

            currentTorrentSources = stream.sources
            val trackers = stream.sources
                ?.filter { it.startsWith("tracker:") }
                ?.map { it.removePrefix("tracker:") }
                ?: emptyList()
            val localUrl = startTorrentStream(
                infoHash = infoHash,
                fileIdx = stream.getEffectiveFileIdx(),
                filename = stream.behaviorHints?.filename,
                trackers = trackers
            )

            currentStreamUrl = localUrl
            currentHeaders = emptyMap()
            currentStreamMimeType = null
            recordDistinctPluginPlay(
                bingeGroup = stream.behaviorHints?.bingeGroup ?: currentStreamBingeGroup,
                playbackIdentity = infoHash
            )

            preparePlaybackBeforeStart(
                url = localUrl,
                headers = emptyMap(),
                loadSavedProgress = loadSavedProgress,
                startPaused = startPaused
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start torrent stream", e)
            _uiState.update {
                it.copy(
                    error = context.getString(
                        R.string.player_error_failed_start_torrent,
                        e.message ?: context.getString(R.string.error_unknown)
                    ),
                    showLoadingOverlay = false,
                    loadingProgress = null
                )
            }
        }
    }
}

private fun formatSpeed(context: android.content.Context, bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1_048_576 -> context.getString(R.string.unit_speed_mb_s, String.format("%.1f", bytesPerSec / 1_048_576.0))
        bytesPerSec >= 1_024 -> context.getString(R.string.unit_speed_kb_s, String.format("%.0f", bytesPerSec / 1_024.0))
        else -> context.getString(R.string.unit_speed_b_s, bytesPerSec)
    }
}

private fun formatMB(context: android.content.Context, bytes: Long): String =
    context.getString(R.string.unit_size_mb, String.format("%.1f", bytes / 1_048_576.0))
