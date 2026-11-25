package com.kien.movies.streams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor

/**
 * A capsule bar (tvOS WatchProgressBar) in the content colour, also the torrent buffer bar of a row. Private to Streams:
 * Home and Details keep their own until it moves to design/.
 */
@Composable
fun StreamsProgressBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = BAR_HEIGHT) {
    val content = LocalContentColor.current
    Box(modifier = modifier.height(height).background(content.copy(alpha = TRACK_ALPHA), CircleShape)) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(content, CircleShape)
        )
    }
}

private val BAR_HEIGHT = 4.dp // WatchProgressBar height 8

// The track is the platter's 22% of the content colour, so it shows on a focused white row too.
private const val TRACK_ALPHA = 0.22f
