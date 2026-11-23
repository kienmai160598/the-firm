package com.kien.movies.streams

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.GlassStyle
import com.kien.movies.design.Palette
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles
import com.nuvio.tvos.TvosStreamsEmptyState

/**
 * No addon found a stream (tvOS StreamsEmptyView.swift, a ContentUnavailableView): symbol, the
 * Kotlin title and message, the missing addon hint, and Try Again, which holds focus (S3).
 */
@Composable
fun StreamsEmpty(emptyState: TvosStreamsEmptyState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = EMPTY_MAX_WIDTH),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.textGap)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_streams_unavailable),
                contentDescription = null,
                tint = Palette.textSecondary,
                modifier = Modifier.size(EMPTY_SYMBOL)
            )
            Text(text = emptyState.title, style = TextStyles.shelfHeader.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
            Text(text = emptyState.message, style = TextStyles.body, color = Palette.textSecondary, textAlign = TextAlign.Center)
            if (emptyState.isMissingStreamAddon) {
                Text(text = MISSING_ADDON_HINT, style = TextStyles.body, color = Palette.textTertiary, textAlign = TextAlign.Center)
            }
            StreamsRetryButton(onRetry = onRetry, modifier = Modifier.testTag(STREAMS_RETRY_TAG))
        }
    }
}

/** Try Again, also the focus of a list where every addon failed (Android only, see StreamGroupList). */
@Composable
fun StreamsRetryButton(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onRetry,
        colors = GlassStyle.buttonColors(),
        scale = GlassStyle.buttonScale(),
        modifier = modifier
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.textGap), verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.ic_streams_retry), contentDescription = null, modifier = Modifier.size(BUTTON_SYMBOL))
            Text(text = "Try Again", style = TextStyles.button)
        }
    }
}

/** Before the first addon answers (tvOS StreamsLoadingView): a focusable spinner and label. */
@Composable
fun StreamsLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.testTag(STREAMS_LOADING_TAG).focusable(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.textGapLarge)
        ) {
            StreamsSpinner()
            Text(text = "Finding streams", style = TextStyles.body.copy(fontWeight = FontWeight.Medium), color = Palette.textSecondary)
        }
    }
}

/**
 * The system activity indicator stand-in (tv-material has none): a quarter arc that turns. Only
 * the small arc is redrawn each frame.
 */
@Composable
fun StreamsSpinner(modifier: Modifier = Modifier, size: Dp = SPINNER_SIZE) {
    val turn = rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = FULL_TURN,
        animationSpec = infiniteRepeatable(tween(SPINNER_TURN_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "spinnerTurn"
    )
    val color = LocalContentColor.current
    // The turn is read in the draw phase only, so the spinner never recomposes.
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = SPINNER_STROKE.toPx())
        drawArc(color = color.copy(alpha = SPINNER_TRACK_ALPHA), startAngle = 0f, sweepAngle = FULL_TURN, useCenter = false, style = stroke)
        rotate(turn.value) {
            drawArc(color = color, startAngle = 0f, sweepAngle = SPINNER_SWEEP, useCenter = false, style = stroke)
        }
    }
}

const val STREAMS_RETRY_TAG = "streams-retry"
const val STREAMS_LOADING_TAG = "streams-loading"

private const val MISSING_ADDON_HINT =
    "None of your installed addons provide streams. Cinemeta only provides catalogs " +
        "and details. Install a stream addon to see sources here."

// tvOS literals, pt / 2.
private val EMPTY_MAX_WIDTH = 500.dp // frame(maxWidth: 1000)
private val EMPTY_SYMBOL = 40.dp // ContentUnavailableView symbol, about 80 pt
private val BUTTON_SYMBOL = 16.dp
private val SPINNER_SIZE = 20.dp // ProgressView, about 40 pt
private val SPINNER_STROKE = 2.dp
private const val SPINNER_TURN_MILLIS = 900
private const val SPINNER_SWEEP = 90f
private const val SPINNER_TRACK_ALPHA = 0.25f
private const val FULL_TURN = 360f
