package com.kien.movies.player

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kien.movies.shell.PlaceholderScreen

/** The player UI (tvOS MpvPlayerScreen.swift). Stub until slice A8: black, one button, Back exits. */
@Composable
fun PlayerScreen(request: PlaybackRequest, onFailure: (StreamNotice) -> Unit, onExit: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel = viewModel(key = request.id) { PlayerViewModel(application, request) }
    BackHandler(onBack = onExit)
    PlaceholderScreen(
        title = "Player",
        detail = "${viewModel.request.target.title}, coming in A8",
        modifier = Modifier.fillMaxSize().background(Color.Black)
    )
}
