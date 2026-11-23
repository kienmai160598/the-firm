package com.kien.movies.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.kien.movies.ui.theme.ExpressiveMotion

private val RestingTrackHeight = 4.dp
private val FocusedTrackHeight = 8.dp
private val ThumbRadius = 9.dp
private val InactiveTrackColor = Color.White.copy(alpha = 0.24f)

// Drawn over the inactive track, so the buffered part shows white at about 0.4.
private val BufferedTrackColor = Color.White.copy(alpha = 0.21f)
private val ThumbColor = Color.White

/**
 * The Apple TV scrubber with Netflix's red played part: a thin capsule track that thickens on
 * focus and gains a white thumb. Progress and the focus spring are read only while measuring
 * and drawing, so animating either costs no recomposition.
 */
@Composable
internal fun Modifier.expressiveSeekTrack(
    focused: Boolean,
    progress: () -> Float,
    buffered: () -> Float
): Modifier {
    val emphasis = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = ExpressiveMotion.standardDefaultSpatial(),
        label = "seekTrackEmphasis"
    )
    val playedColor = PlayerSurfaceStyle.playedColor()

    return this
        .layout { measurable, constraints ->
            val fraction = emphasis.value.coerceIn(0f, 1f)
            val height = lerp(RestingTrackHeight, FocusedTrackHeight, fraction).roundToPx()
            val placeable = measurable.measure(
                constraints.copy(minHeight = height, maxHeight = height)
            )
            layout(placeable.width, height) { placeable.place(0, 0) }
        }
        .drawBehind {
            drawSeekTrack(
                emphasis = emphasis.value.coerceIn(0f, 1f),
                progress = progress().coerceIn(0f, 1f),
                buffered = buffered().coerceIn(0f, 1f),
                playedColor = playedColor
            )
        }
}

private fun DrawScope.drawSeekTrack(
    emphasis: Float,
    progress: Float,
    buffered: Float,
    playedColor: Color
) {
    val playedX = size.width * progress

    drawTrackSegment(InactiveTrackColor, 0f, size.width)
    drawTrackSegment(BufferedTrackColor, 0f, size.width * buffered)
    drawTrackSegment(playedColor, 0f, playedX)
    drawThumb(playedX, emphasis)
}

private fun DrawScope.drawTrackSegment(color: Color, start: Float, end: Float) {
    val left = start.coerceAtLeast(0f)
    if (end <= left) return

    drawRoundRect(
        color = color,
        topLeft = Offset(left, 0f),
        size = Size(end - left, size.height),
        cornerRadius = CornerRadius(size.height / 2f)
    )
}

private fun DrawScope.drawThumb(centerX: Float, emphasis: Float) {
    if (emphasis <= 0f) return

    drawCircle(
        color = ThumbColor,
        radius = ThumbRadius.toPx() * emphasis,
        center = Offset(centerX, size.height / 2f),
        alpha = emphasis
    )
}
