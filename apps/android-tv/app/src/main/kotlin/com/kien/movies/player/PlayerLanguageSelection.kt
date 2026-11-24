package com.kien.movies.player

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import com.nuvio.tvos.TvosPlayerLanguages

/**
 * The Settings languages as ExoPlayer preferences (tvOS sets mpv's `alang` and `slang` the same
 * way): the audio and subtitle languages to try, in order, and no subtitles when Settings says Off.
 * They are preferences only, so a track picked in the player menu still wins through its override.
 */
internal object PlayerLanguageSelection {
    fun applied(current: TrackSelectionParameters, languages: TvosPlayerLanguages): TrackSelectionParameters {
        val builder = current.buildUpon().setPreferredAudioLanguages(*languages.audio.toTypedArray())
        if (languages.showsSubtitles) {
            builder.setPreferredTextLanguages(*languages.subtitles.toTypedArray()).setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        }
        return builder.build()
    }
}
