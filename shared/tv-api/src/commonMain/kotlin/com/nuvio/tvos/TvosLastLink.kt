package com.nuvio.tvos

import com.nuvio.app.features.streams.StreamLinkCacheStorage
import com.nuvio.app.features.streams.epochMs
import com.nuvio.tvos.rules.LastLink

/**
 * The link each movie or episode last played from, so its Continue Watching card plays it again
 * without the Streams list (AC H4). Kept in core's stream link storage under keys of its own, so
 * core's own link cache never reads them.
 */
object TvosLastLink {
    /** The recalled stream's key, apart from the keys of the rows in the Streams list. */
    const val RECALLED_KEY = "last-link"

    /** Called once a playback has saved progress, so a link that never played is not kept. */
    fun remember(target: TvosStreamTarget, stream: TvosStream) {
        StreamLinkCacheStorage.saveEntry(key(target), LastLink.encode(stream, epochMs()))
    }

    /** The link played within the last day, or null. */
    fun recall(target: TvosStreamTarget): TvosStream? {
        val payload = StreamLinkCacheStorage.loadEntry(key(target)) ?: return null
        return LastLink.decode(payload, epochMs())?.copy(key = RECALLED_KEY) ?: run {
            forget(target)
            null
        }
    }

    /** Drops the link, after it failed to play. */
    fun forget(target: TvosStreamTarget) {
        StreamLinkCacheStorage.removeEntry(key(target))
    }

    private fun key(target: TvosStreamTarget): String = listOf(
        "tv_last_link",
        target.type.lowercase(),
        target.parentMetaId,
        target.seasonNumber?.toString().orEmpty(),
        target.episodeNumber?.toString().orEmpty(),
        target.videoId,
    ).joinToString("|")
}
