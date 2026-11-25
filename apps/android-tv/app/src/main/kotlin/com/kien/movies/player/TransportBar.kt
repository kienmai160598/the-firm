package com.kien.movies.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.kien.movies.design.TvColors
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
private val SkipGap = 8.dp
private val SkipGlyphGap = 4.dp
private val PauseIconSize = 15.dp // 30 pt
private val LabelStyle = TvType.caption2.copy(fontFeatureSettings = "tnum") // AVKit time labels, Caption 2
private const val SCRUB_ANIMATION_MILLIS = 200

/**
 * The transport bar (tvOS MpvTransportBar.swift): track with buffered and played fills, a
 * playhead while scrubbing, the row under the playhead (elapsed time, pause icon, skip glyph) and
 * the remaining time at the end. The fills are drawn in one Canvas and read their fractions in the
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
            Canvas(modifier = Modifier.fillMaxWidth().height(TrackSlot)) {
                val height = (RestHeight + (ScrubHeight - RestHeight) * trackHeight).toPx()
                val top = (size.height - height) / 2
                val radius = CornerRadius(height / 2)
                drawRoundRect(TrackFill, Offset(0f, top), Size(size.width, height), radius)
                drawRoundRect(BufferedFill, Offset(0f, top), Size(size.width * current.fraction(current.bufferedSeconds), height), radius)
                val playedFill = if (isScrubbing) PlayedScrubFill else PlayedFill
                drawRoundRect(playedFill, Offset(0f, top), Size(size.width * current.fraction(current.positionSeconds), height), radius)
                drawRoundRect(TrackEdge, Offset(0f, top), Size(size.width, height), radius, style = Stroke(TrackEdgeWidth.toPx()))
                if (isScrubbing || previewSeconds != null) drawPlayhead(size.width * current.fraction(shownSeconds))
            }
            if (current.durationSeconds > 0) {
                Box(modifier = Modifier.padding(top = LabelGap).fillMaxWidth().height(LabelRowHeight)) {
                    ElapsedRow(
                        seconds = shownSeconds,
                        showsPause = isPausedByViewer && previewSeconds == null,
                        skip = skip,
                        playheadX = playheadX,
                        width = width,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
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

/**
 * The row under the playhead. At rest: the elapsed time, or the pause icon while paused, centred
 * on the playhead. After a skip: the skip glyph and the time beside that anchor, on the right
 * for forward and on the left for back, so they never sit on top of the pause icon.
 */
@Composable
private fun ElapsedRow(seconds: Int, showsPause: Boolean, skip: SkipBadge?, playheadX: Dp, width: Dp, modifier: Modifier) {
    var rowWidth by remember { mutableIntStateOf(0) }
    val placement = modifier
        .onSizeChanged { rowWidth = it.width }
        .offset {
            val anchorHalf = if (showsPause) (PauseIconSize / 2).roundToPx() else 0
            val playhead = playheadX.roundToPx()
            val start = when {
                skip == null -> playhead - rowWidth / 2
                skip.isForward -> playhead - anchorHalf
                else -> playhead + anchorHalf - rowWidth
            }
            val last = (width - RemainingReserve).roundToPx() - rowWidth
            IntOffset(start.coerceAtMost(last.coerceAtLeast(0)).coerceAtLeast(0), 0)
        }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SkipGap), modifier = placement) {
        if (skip?.isForward == false) SkippedTime(skip, seconds)
        if (showsPause) PauseIcon()
        if (skip == null && !showsPause) ElapsedTime(seconds)
        if (skip?.isForward == true) SkippedTime(skip, seconds)
    }
}

@Composable
private fun PauseIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_player_pause_circle),
        contentDescription = "Paused",
        modifier = Modifier.size(PauseIconSize).testTag(PLAYER_PAUSED_TAG)
    )
}

@Composable
private fun ElapsedTime(seconds: Int) {
    Text(text = clockLabel(seconds), style = LabelStyle)
}

/** The skip glyph then the time it landed on, as one group beside the pause icon. */
@Composable
private fun SkippedTime(skip: SkipBadge, seconds: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SkipGlyphGap)) {
        SkipBadgeIcon(skip)
        ElapsedTime(seconds)
    }
}

/**
 * The playhead, centred on the track and taller than it by [PlayheadRise]. Drawn, not laid out:
 * a laid out box would be clipped to the track's height by its constraints.
 */
private fun DrawScope.drawPlayhead(x: Float) {
    val width = PlayheadWidth.toPx()
    val height = (TrackSlot + PlayheadRise).toPx()
    drawRoundRect(Color.White, Offset(x - width / 2, (size.height - height) / 2), Size(width, height), CornerRadius(width / 2))
}

/**
 * tvOS MpvSkipBadge: the "10" arrow of the direction, its number part of the glyph so nothing
 * overlaps it, bouncing on every press while presses add up.
 */
@Composable
private fun SkipBadgeIcon(badge: SkipBadge) {
    val description = if (badge.isForward) "Skipped forward" else "Skipped back"
    val glyph = if (badge.isForward) R.drawable.ic_player_skip_forward_10 else R.drawable.ic_player_skip_back_10
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(badge.presses) {
        if (badge.presses > 1) {
            bounce.snapTo(SKIP_BOUNCE_SCALE)
            bounce.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(SkipGlyphSize).testTag(PLAYER_SKIP_TAG).semantics { contentDescription = description }
    ) {
        Icon(
            painter = painterResource(glyph),
            contentDescription = null,
            modifier = Modifier.size(SkipGlyphSize).graphicsLayer {
                scaleX = bounce.value
                scaleY = bounce.value
            }
        )
    }
}

private val SkipGlyphSize = 22.dp // 44 pt
private const val SKIP_BOUNCE_SCALE = 1.2f

// tvOS MpvTransportBar.track: a glass capsule (grey as measured over video) with white fills and a 1 pt white edge at 18%. The
// track is a few dp tall, so its translucent fills cost the box next to nothing.
private val TrackFill = TvColors.playerTrack
private val BufferedFill = Color.White.copy(alpha = 0.3f)
private val PlayedFill = Color.White.copy(alpha = 0.9f)
private val PlayedScrubFill = Color.White.copy(alpha = 0.55f)
private val TrackEdge = Color.White.copy(alpha = 0.18f)
private val TrackEdgeWidth = 0.5.dp // 1 pt
