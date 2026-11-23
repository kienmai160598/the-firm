package com.kien.movies.player

import android.app.Application
import androidx.lifecycle.ViewModel

/**
 * Owns the ExoPlayer instance and the player state for one [PlaybackRequest] (tvOS
 * MpvPlaybackController.swift). Stub until slice A8. Create it with
 * `viewModel(key = request.id) { PlayerViewModel(application, request) }`: the cover lives in the
 * Streams entry's ViewModel store, so the key keeps two playbacks apart.
 */
class PlayerViewModel(val application: Application, val request: PlaybackRequest) : ViewModel()
