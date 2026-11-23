package com.kien.movies.player

import androidx.compose.runtime.Composable

/**
 * The full screen player cover that Streams presents (tvOS PlaybackScreen.swift). Frozen
 * signature, owned body.
 *
 * @param onFailure the stream could not start or stalled; Streams closes the cover and shows the
 *   notice.
 * @param onExit the user left the player (Back at the end of the six step order, or playback ended).
 */
@Composable
fun PlaybackScreen(request: PlaybackRequest, onFailure: (StreamNotice) -> Unit, onExit: () -> Unit) {
    PlayerScreen(request = request, onFailure = onFailure, onExit = onExit)
}
