package com.nuvio.tvos

import com.nuvio.app.features.p2p.P2pStreamRequest
import com.nuvio.app.features.p2p.P2pStreamingState
import com.nuvio.app.features.p2p.formatP2pSpeed

internal const val TORRENT_PHASE_IDLE = "idle"
internal const val TORRENT_PHASE_CONNECTING = "connecting"
internal const val TORRENT_PHASE_STREAMING = "streaming"
internal const val TORRENT_PHASE_FAILED = "failed"

internal const val NO_INFO_HASH = "This stream has no torrent info hash"
internal const val START_FAILED = "Unable to start torrent stream"

internal fun TvosStream.torrentRequest(): P2pStreamRequest? {
    val hash = infoHash?.takeIf(String::isNotBlank) ?: return null
    return P2pStreamRequest(
        infoHash = hash,
        fileIdx = fileIndex?.toIntOrNull(),
        filename = filename,
        trackers = trackers,
    )
}

internal fun tvosTorrentStatus(state: P2pStreamingState): TvosTorrentStatus =
    when (state) {
        P2pStreamingState.Idle -> quietStatus(TORRENT_PHASE_IDLE, errorMessage = null)
        is P2pStreamingState.Error -> quietStatus(TORRENT_PHASE_FAILED, state.message)
        is P2pStreamingState.Connecting -> TvosTorrentStatus(
            phase = TORRENT_PHASE_CONNECTING,
            step = state.phase,
            localUrl = null,
            peers = state.peers,
            seeds = state.seeds,
            downloadSpeedLabel = formatP2pSpeed(state.downloadSpeed),
            bufferFraction = 0f,
            downloadedFraction = 0f,
            errorMessage = null,
        )
        is P2pStreamingState.Streaming -> TvosTorrentStatus(
            phase = TORRENT_PHASE_STREAMING,
            step = null,
            localUrl = state.localUrl,
            peers = state.peers,
            seeds = state.seeds,
            downloadSpeedLabel = formatP2pSpeed(state.downloadSpeed),
            bufferFraction = state.bufferProgress,
            downloadedFraction = state.totalProgress,
            errorMessage = null,
        )
    }

internal fun quietStatus(phase: String, errorMessage: String?): TvosTorrentStatus =
    TvosTorrentStatus(
        phase = phase,
        step = null,
        localUrl = null,
        peers = 0,
        seeds = 0,
        downloadSpeedLabel = formatP2pSpeed(0L),
        bufferFraction = 0f,
        downloadedFraction = 0f,
        errorMessage = errorMessage,
    )
