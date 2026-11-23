package com.kien.movies.streams

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kien.movies.player.PlaybackRequest
import com.kien.movies.player.PlaybackScreen
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.PlaceholderScreen
import com.kien.movies.shell.Router

/**
 * Streams (tvOS StreamsView.swift). Stub until slice A7; the signature stays. Streams presents the
 * player as a full screen cover over itself, as tvOS does with fullScreenCover: the stub shows
 * the seam the two slices meet at.
 */
@Composable
fun StreamsScreen(route: AppRoute.Streams, router: Router) {
    val viewModel = viewModel { StreamsViewModel(route.target) }
    var playback by remember { mutableStateOf<PlaybackRequest?>(null) }
    Box {
        PlaceholderScreen(title = "Streams", detail = "${viewModel.target.title}, coming in A7")
        playback?.let { request ->
            PlaybackScreen(
                request = request,
                onFailure = { playback = null },
                onExit = { playback = null }
            )
        }
    }
}
