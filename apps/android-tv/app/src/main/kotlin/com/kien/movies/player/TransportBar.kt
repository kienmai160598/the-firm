package com.kien.movies.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.PlatterOverBlack
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvType
import kotlinx.coroutines.flow.StateFlow

/** Playback position for the bar, in whole seconds (tvOS MpvPlaybackClock.swift). */
data class PlaybackClock(val positionSeconds: Int = 0, val durationSeconds: Int = 0, val bufferedSeconds: Int = 0) {
    fun fraction(seconds: Int): Float = if (durationSeconds <= 0) 0f else (seconds.toFloat() / durationSeconds).coerceIn(0f, 1f)
}

/** "4:05" or "1:02:03" (tvOS MpvClock.label). */
fun clockLabel(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = totalSeconds / 60 % 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

const val PLAYER_REMAINING_TAG = "player-remaining"
const val PLAYER_SKIP_TAG = "player-skip"
const val PLAYER_PAUSED_TAG = "player-paused"

// tvOS MpvTransportBar.swift literals, pt / 2.
private val RestHeight = 6.dp // 12 pt
private val ScrubHeight = 9.dp // 18 pt
private val TrackSlot = 9.dp // 18 pt
private val RemainingReserve = 90.dp // 180 pt
private val LabelRowHeight = 22.dp // 44 pt
private val LabelGap = 6.dp // 12 pt
private val PlayheadWidth = 2.dp // 4 pt
private val PlayheadRise = 5.dp // 10 pt
private val SkipBadgeSize = 32.dp // 64 pt
private val SkipRise = 20.dp // 40 pt
private val PauseIconSize = 15.dp // 30 pt
private val LabelStyle = TvType.caption2.copy(fontFeatureSettings = "tnum") // AVKit time labels, Caption 2
private const val SCRUB_ANIMATION_MILLIS = 200

/**
 * The transport bar (tvOS MpvTransportBar.swift): track with buffered and played fills, a
 * playhead while scrubbing, elapsed time under the playhead (or the pause icon), remaining time at
 * the end, and the skip badge. The fills are drawn in one Canvas and read their fractions in the
 * draw phase, so the per second clock does not re-measure anything.
 */
@Composable
fun TransportBar(
    clock: StateFlow<PlaybackClock>,
    previewSeconds: Int?,
    isPausedByViewer: Boolean,
    isScrubbing: Boolean,
    skip: SkipBadge?,
    modifier: Modifier = Modifier
) {
    val current by clock.collectAsStateWithLifecycle()
    val shownSeconds = previewSeconds ?: current.positionSeconds
    val trackHeight by animateFloatAsState(if (isScrubbing) 1f else 0f, tween(SCRUB_ANIMATION_MILLIS), label = "trackHeight")
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val width = maxWidth
        val playheadX = width * current.fraction(shownSeconds)
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(TrackSlot)) {
                Canvas(modifier = Modifier.fillMaxWidth().height(TrackSlot)) {
                    val height = (RestHeight + (ScrubHeight - RestHeight) * trackHeight).toPx()
                    val top = (size.height - height) / 2
                    val radius = CornerRadius(height / 2)
                    drawRoundRect(TrackFill, Offset(0f, top), Size(size.width, height), radius)
                    drawRoundRect(BufferedFill, Offset(0f, top), Size(size.width * current.fraction(current.bufferedSeconds), height), radius)
                    val playedFill = if (isScrubbing) PlayedScrubFill else PlayedFill
                    drawRoundRect(playedFill, Offset(0f, top), Size(size.width * current.fraction(current.positionSeconds), height), radius)
                }
                if (isScrubbing || previewSeconds != null) {
                    Box(
                        modifier = Modifier
                            .offset(x = playheadX - PlayheadWidth / 2, y = -PlayheadRise / 2)
                            .size(PlayheadWidth, TrackSlot + PlayheadRise)
                            .background(Color.White, TvShapes.pill)
                    )
                }
                // Qualified: inside the Column, the ColumnScope overload would be picked.
                androidx.compose.animation.AnimatedVisibility(
                    visible = skip != null,
                    enter = fadeIn() + scaleIn(initialScale = SKIP_START_SCALE),
                    exit = fadeOut() + scaleOut(targetScale = SKIP_START_SCALE),
                    modifier = Modifier.offset(x = (playheadX - SkipBadgeSize / 2).coerceIn(0.dp, width - SkipBadgeSize), y = -(SkipRise + SkipBadgeSize / 2))
                ) {
                    skip?.let { badge -> SkipBadgeIcon(badge) }
                }
            }
            if (current.durationSeconds > 0) {
                Box(modifier = Modifier.padding(top = LabelGap).fillMaxWidth().height(LabelRowHeight)) {
                    ElapsedLabel(shownSeconds, showsPause = isPausedByViewer && previewSeconds == null, playheadX = playheadX, width = width)
                    Text(
                        text = "-" + clockLabel((current.durationSeconds - shownSeconds).coerceAtLeast(0)),
                        style = LabelStyle,
                        textAlign = TextAlign.End,
                        modifier = Modifier.align(Alignment.CenterEnd).testTag(PLAYER_REMAINING_TAG)
                    )
                }
            }
        }
    }
}

@Composable
private fun ElapsedLabel(seconds: Int, showsPause: Boolean, playheadX: Dp, width: Dp) {
    var labelWidth by remember { mutableIntStateOf(0) }
    val placement = Modifier
        .onSizeChanged { labelWidth = it.width }
        .offset {
            val centered = playheadX.roundToPx() - labelWidth / 2
            val last = (width - RemainingReserve).roundToPx() - labelWidth
            IntOffset(centered.coerceAtMost(last.coerceAtLeast(0)).coerceAtLeast(0), 0)
        }
    if (showsPause) {
        Icon(
            painter = painterResource(R.drawable.ic_player_pause_circle),
            contentDescription = "Paused",
            modifier = placement.size(PauseIconSize).testTag(PLAYER_PAUSED_TAG)
        )
    } else {
        Text(text = clockLabel(seconds), style = LabelStyle, modifier = placement)
    }
}

@Composable
private fun SkipBadgeIcon(badge: SkipBadge) {
    val description = if (badge.isForward) "Skipped forward" else "Skipped back"
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(SkipBadgeSize).testTag(PLAYER_SKIP_TAG).semantics { contentDescription = description }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_player_skip_back),
            contentDescription = null,
            modifier = Modifier.size(SkipBadgeSize).graphicsLayer { scaleX = if (badge.isForward) -1f else 1f }
        )
        Text(text = "${badge.seconds}", style = TvType.overline, modifier = Modifier.offset(y = SkipNumberDrop))
    }
}

private val SkipNumberDrop = 2.dp

// Opaque fills (composited over black) so the bar adds no translucent layer over video on the box.
private val TrackFill = PlatterOverBlack
private val BufferedFill = TvColors.textTertiary.compositeOver(Color.Black)
private val PlayedFill = Color.White.copy(alpha = 0.9f).compositeOver(Color.Black)
private val PlayedScrubFill = Color.White.copy(alpha = 0.55f).compositeOver(Color.Black)
private const val SKIP_START_SCALE = 0.7f
