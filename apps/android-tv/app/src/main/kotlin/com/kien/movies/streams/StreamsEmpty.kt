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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.Focus
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvControls
import com.kien.movies.design.TvGlass
import com.kien.movies.design.TvType
import com.kien.movies.design.focusLift
import com.nuvio.tvos.TvosStreamsEmptyState

/**
 * No addon found a stream (tvOS StreamsEmptyView.swift): the spec's empty state (Headline bold
 * title, Caption 2 secondary message, centred, no symbol), the missing addon hint and Try Again,
 * which holds focus (S3).
 */
@Composable
fun StreamsEmpty(emptyState: TvosStreamsEmptyState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = EMPTY_MAX_WIDTH),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emptyState.title, style = TvType.headline, textAlign = TextAlign.Center)
            Text(
                text = emptyState.message,
                style = TvType.caption2,
                color = TvColors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.paddingFromBaseline(top = MESSAGE_BASELINE)
            )
            if (emptyState.isMissingStreamAddon) {
                Text(
                    text = MISSING_ADDON_HINT,
                    style = TvType.caption2,
                    color = TvColors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = HINT_TOP)
                )
            }
            StreamsRetryButton(onRetry = onRetry, modifier = Modifier.padding(top = BUTTON_TOP).testTag(STREAMS_RETRY_TAG))
        }
    }
}

/** Try Again, also the focus of a list where every addon failed (Android only, see StreamGroupList). */
@Composable
fun StreamsRetryButton(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onRetry,
        colors = TvGlass.buttonColors(),
        scale = TvGlass.noButtonScale(),
        shape = TvGlass.pillShape(),
        border = TvGlass.buttonBorder(),
        contentPadding = TvGlass.pillPadding,
        modifier = modifier.height(TvControls.pillHeight).focusLift(Focus.buttonScale)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(GLYPH_GAP), verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.ic_streams_retry), contentDescription = null, modifier = Modifier.size(TvControls.glyph))
            Text(text = "Try Again", style = TvType.bodyEmphasized)
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
            verticalArrangement = Arrangement.spacedBy(EMPTY_GAP)
        ) {
            StreamsSpinner()
            Text(text = "Finding streams", style = TvType.caption2, color = TvColors.textSecondary)
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
private val EMPTY_GAP = 12.dp

// 61-library.png: title baseline 451 pt, message baseline 535 pt, 42 dp apart; the title's line
// box ends about 5 dp under its baseline.
private val MESSAGE_BASELINE = 37.dp
private val HINT_TOP = 8.dp
private val BUTTON_TOP = 20.dp
private val GLYPH_GAP = 8.dp
private val SPINNER_SIZE = 20.dp // ProgressView, about 40 pt
private val SPINNER_STROKE = 2.dp
private const val SPINNER_TURN_MILLIS = 900
private const val SPINNER_SWEEP = 90f
private const val SPINNER_TRACK_ALPHA = 0.25f
private const val FULL_TURN = 360f
