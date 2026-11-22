package com.kien.movies.ui.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.ui.theme.NuvioTheme
import com.kien.movies.ui.theme.isExpressiveBuild

private const val TRANSPORT_SUPPORTING_ALPHA = 0.64f
private const val TABULAR_FIGURES = "tnum"

/** The Apple TV transport keeps the clock on the scrubber, so the button row drops its own. */
internal val playerTimesUnderScrubber: Boolean = isExpressiveBuild

/**
 * The bottom scrim behind the transport: clear at the top, deepening to near black at the edge,
 * so the title, scrubber and buttons read over any frame. Cached, so it never redraws per frame.
 */
internal fun Modifier.playerTransportScrim(): Modifier = drawWithCache {
    val scrim = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Color.Transparent,
            0.35f to Color.Black.copy(alpha = 0.4f),
            1f to Color.Black.copy(alpha = 0.78f)
        )
    )
    onDrawBehind { drawRect(scrim) }
}

/** Supporting lines of the transport, such as the episode line and the year. */
internal val PlayerTransportSupporting: Color = Color.White.copy(alpha = TRANSPORT_SUPPORTING_ALPHA)

/** Elapsed time under the scrubber's start, time left under its end. */
@Composable
internal fun PlayerScrubberTimesHost(viewModel: PlayerViewModel) {
    val playbackTimeline by viewModel.playbackTimeline.collectAsState()
    val remainingMs = (playbackTimeline.duration - playbackTimeline.currentPosition)
        .coerceAtLeast(0L)

    PlayerScrubberTimes(
        elapsed = formatTime(playbackTimeline.currentPosition),
        remaining = "-${formatTime(remainingMs)}"
    )
}

@Composable
private fun PlayerScrubberTimes(elapsed: String, remaining: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = NuvioTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerScrubberTime(elapsed)
        PlayerScrubberTime(remaining)
    }
}

@Composable
private fun PlayerScrubberTime(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            fontFeatureSettings = TABULAR_FIGURES
        ),
        color = PlayerTransportSupporting
    )
}
