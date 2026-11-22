package com.kien.movies.ui.screens.player

import java.util.Locale

/**
 * Whether the subtitle the user has selected is ASS/SSA. The libass overlay surface is only
 * attached while this holds: an idle full-screen overlay still redraws on every video frame and
 * keeps the video off the hardware composer plane.
 */
internal fun PlayerUiState.isSelectedSubtitleAss(): Boolean {
    val addon = selectedAddonSubtitle
    if (addon != null) {
        val url = addon.url.lowercase(Locale.US)
        return url.contains(".ass") || url.contains(".ssa")
    }

    val track = subtitleTracks.getOrNull(selectedSubtitleTrackIndex) ?: return false
    val codec = track.codec?.lowercase(Locale.US).orEmpty()
    return codec.contains("ass") || codec.contains("ssa") ||
        track.name.contains("ASS", ignoreCase = true)
}
