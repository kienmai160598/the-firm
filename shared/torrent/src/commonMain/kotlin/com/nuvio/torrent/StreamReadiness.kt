package com.nuvio.torrent

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** The engine never calls back, so readiness is polled; it changes once, within seconds. */
val streamPollInterval: Duration = 200.milliseconds

/** How long a torrent may take to deliver its metadata before the stream gives up. */
val streamResolveTimeout: Duration = 90.seconds

class TorrentStreamException(message: String) : Exception(message)

/**
 * Waits until [stream] is ready to play. Throws [TorrentStreamException] when it fails or does not
 * resolve within [timeout]; a cancelled caller should close the stream.
 */
suspend fun TorrentEngine.awaitReady(
    stream: Long,
    pollInterval: Duration = streamPollInterval,
    timeout: Duration = streamResolveTimeout
): StreamInfo.Ready {
    val outcome = withTimeoutOrNull(timeout) {
        var info = streamInfo(stream)
        while (info == StreamInfo.Resolving) {
            delay(pollInterval)
            info = streamInfo(stream)
        }
        info
    }
    return when (outcome) {
        is StreamInfo.Ready -> outcome
        is StreamInfo.Failed -> throw TorrentStreamException(outcome.message)
        else -> throw TorrentStreamException("No torrent metadata after $timeout")
    }
}
