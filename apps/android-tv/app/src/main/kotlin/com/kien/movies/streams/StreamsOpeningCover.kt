package com.kien.movies.streams

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor

/**
 * What a Continue Watching card shows while its last link's torrent starts (tvOS
 * StreamsOpeningCover): the player's loading look, black with the spinner, so the card seems to
 * open the player at once; the list is not composed until the link fails (AC H4). It holds the
 * focus, and Back leaves Streams as from the list.
 */
@Composable
fun StreamsOpeningCover() {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos {}
        focus.requestFocus()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .testTag(STREAMS_OPENING_COVER_TAG),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            StreamsSpinner(size = PLAYER_SPINNER)
        }
    }
}

const val STREAMS_OPENING_COVER_TAG = "streams-opening-cover"

private val PLAYER_SPINNER = 24.dp // the player's ProgressView scaled 1.6
