package com.kien.movies.player

import com.nuvio.tvos.TvosStream
import com.nuvio.tvos.TvosStreamTarget
import java.util.UUID

// The seam between streams/ and player/ (frozen, see apps/android-tv/OWNERS-WAVE.md).

/**
 * One playback the Streams screen asks for (tvOS PlaybackRequest.swift, without the engine
 * choice: ExoPlayer plays everything in v1). [url] is the stream's playable URL, or the local
 * URL of a started torrent.
 */
data class PlaybackRequest(
    val target: TvosStreamTarget,
    val stream: TvosStream,
    val url: String,
    val resumes: Boolean,
    val id: String = UUID.randomUUID().toString()
) {
    companion object {
        /** A request for [stream]'s own playable URL, or null when it has none. */
        fun of(target: TvosStreamTarget, stream: TvosStream, resumes: Boolean): PlaybackRequest? = stream.playableUrl?.let { url -> PlaybackRequest(target, stream, url, resumes) }
    }
}

/** Why playback ended early; Streams shows it in the stream's info sheet (AC P4, S4). */
data class StreamNotice(val title: String, val message: String) {
    companion object {
        /** A Continue Watching card's last link did not play (AC H4): the list is the way on. */
        fun lastLinkFailed(reason: StreamNotice?): StreamNotice = StreamNotice("The last link no longer plays", listOfNotNull("Pick a source from the list.", reason?.message).joinToString(" "))
    }
}
