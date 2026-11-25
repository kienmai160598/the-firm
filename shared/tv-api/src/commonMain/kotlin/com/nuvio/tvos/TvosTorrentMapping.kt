package com.nuvio.tvos

import com.nuvio.app.features.p2p.P2pStreamRequest
import com.nuvio.app.features.p2p.formatP2pSpeed

internal const val TORRENT_PHASE_IDLE = "idle"
internal const val TORRENT_PHASE_CONNECTING = "connecting"
internal const val TORRENT_PHASE_STREAMING = "streaming"
internal const val TORRENT_PHASE_FAILED = "failed"

/** The engine's one step before a stream is ready: waiting for the torrent's metadata. */
internal const val TORRENT_STEP_RESOLVING = "resolving"

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
