package com.kien.movies.player

import androidx.media3.common.MediaMetadata
import com.nuvio.tvos.TvosStreamTarget
import com.nuvio.tvos.rules.PlaybackAdvisory

/** What the player tells the system about the title (tvOS PlaybackMetadata.swift). */
internal object PlaybackMetadata {
    fun of(target: TvosStreamTarget): MediaMetadata {
        val advisory = PlaybackAdvisory.of(target.ageRating, target.genres)
        return MediaMetadata.Builder()
            .setTitle(target.title)
            .setSubtitle(target.subtitle)
            .setGenre(advisory?.genreLine)
            .build()
    }
}
