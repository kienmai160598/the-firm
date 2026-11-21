package com.nuvio.tv.core.streams

import com.nuvio.tv.core.debrid.StreamTextSizeParser
import com.nuvio.tv.domain.model.Stream

// Three short samples on the Mi Box averaged 31 to 36 Mbps of download. A stream has to stay
// well under that for its read-ahead to fill faster than playback drains it.
internal const val P2P_STREAM_BITRATE_BUDGET_BITS_PER_SECOND = 25_000_000L

// The size of the one video a stream plays. An addon's structured size is trusted. A size printed
// in the text is trusted for a film, and for an episode only when the stream names its file, since
// a stream without a file index may print the size of a whole season pack.
internal fun p2pVideoSizeBytes(stream: Stream, isEpisode: Boolean): Long? {
    val structuredSize = stream.behaviorHints?.videoSize?.takeIf { it > 0L }
    if (structuredSize != null) return structuredSize
    if (isEpisode && stream.fileIdx == null) return null

    return StreamTextSizeParser.sizeBytesFromStreamText(stream)
}

internal fun p2pStreamBitsPerSecond(stream: Stream, title: TitleDuration): Long? {
    if (!stream.isTorrent()) return null
    val runtimeSeconds = title.runtimeMinutes?.takeIf { it > 0 }?.times(60L) ?: return null
    val sizeBytes = p2pVideoSizeBytes(stream, title.isEpisode) ?: return null

    return sizeBytes * 8L / runtimeSeconds
}

internal fun exceedsP2pBitrateBudget(stream: Stream, title: TitleDuration): Boolean {
    val bitsPerSecond = p2pStreamBitsPerSecond(stream, title) ?: return false

    return bitsPerSecond > P2P_STREAM_BITRATE_BUDGET_BITS_PER_SECOND
}

// Marks the streams this device cannot keep up with and moves them behind the others, keeping the
// addon's own order inside each half.
internal fun rankP2pStreamsForDevice(streams: List<Stream>, title: TitleDuration): List<Stream> =
    streams
        .map { stream ->
            val exceeds = exceedsP2pBitrateBudget(stream, title)
            if (stream.exceedsDeviceBudget == exceeds) stream
            else stream.copy(exceedsDeviceBudget = exceeds)
        }
        .sortedBy { it.exceedsDeviceBudget }
