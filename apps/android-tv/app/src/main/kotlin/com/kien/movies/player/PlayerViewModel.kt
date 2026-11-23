package com.kien.movies.player

import android.app.Application
import androidx.lifecycle.ViewModel

/**
 * Holds the [PlayerController] of one [PlaybackRequest] across configuration changes (tvOS keeps
 * MpvPlaybackController in a StateObject). Create it with
 * `viewModel(key = request.id) { PlayerViewModel(application, request) }`: the cover lives in the
 * Streams entry's ViewModel store, so the key keeps two playbacks apart.
 *
 * That store is cleared only when Streams pops, so [onCleared] is a safety net: the screen calls
 * [release] when the cover leaves, and the second call is a no op.
 */
class PlayerViewModel(application: Application, val request: PlaybackRequest) : ViewModel() {
    val controller = PlayerController(application, request)

    fun release() = controller.finish()

    override fun onCleared() = controller.finish()
}
