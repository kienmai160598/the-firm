package com.kien.movies.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A capsule progress bar (tvOS WatchProgressBar in ContinueWatchingRowView.swift): a track and a
 * white fill, both drawn in one Canvas, no layer. Give it a width; [fraction] is clamped.
 */
@Composable
internal fun WatchProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    track: Color = DEFAULT_TRACK,
    height: Dp = DEFAULT_HEIGHT_8PT
) {
    val filled = progressFraction(fraction)
    Canvas(modifier = modifier.height(height)) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = track, cornerRadius = radius)
        if (filled > 0f) {
            drawRoundRect(color = Color.White, size = Size(size.width * filled, size.height), cornerRadius = radius)
        }
    }
}

private val DEFAULT_TRACK = Color.Black.copy(alpha = 0.55f)
private val DEFAULT_HEIGHT_8PT = 4.dp
