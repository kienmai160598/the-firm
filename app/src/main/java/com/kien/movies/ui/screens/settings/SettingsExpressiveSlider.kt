package com.kien.movies.ui.screens.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveMotion
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.isExpressiveBuild

// Stays under the 38 dp step buttons beside it, so the row keeps its height.
private val SliderHeight = 36.dp
private val TrackThickness = 16.dp
private val TrackInnerCorner = 2.dp
private val HandleGap = 6.dp
private val HandleWidth = 4.dp
private val HandleFocusedWidth = 6.dp
private const val INACTIVE_TRACK_ALPHA = 0.2f

/**
 * The Material 3 Expressive slider: a thick active track in the accent, a tonal inactive track,
 * and a bar handle held apart from both by a gap. The handle slides on the expressive spring and
 * widens while the row is focused. Everything is drawn, so a change only redraws.
 */
@Composable
internal fun SettingsExpressiveSlider(
    progressFraction: Float,
    focused: Boolean,
    contentAlpha: Float,
    modifier: Modifier = Modifier
) {
    val progress = animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = ExpressiveMotion.expressiveFastSpatial(),
        label = "sliderProgress"
    )
    val handleWidth = animateDpAsState(
        targetValue = if (focused) HandleFocusedWidth else HandleWidth,
        animationSpec = ExpressiveMotion.expressiveFastSpatial(),
        label = "sliderHandleWidth"
    )
    val activeColor = sliderActiveColor(focused).copy(alpha = contentAlpha)
    val inactiveColor = sliderInactiveColor(focused, contentAlpha)

    Box(
        modifier = modifier
            .height(SliderHeight)
            .drawWithCache {
                val path = Path()
                onDrawBehind {
                    val handlePx = handleWidth.value.toPx()
                    val handleStart = progress.value.coerceIn(0f, 1f) * (size.width - handlePx)
                    val handleEnd = handleStart + handlePx
                    val gapPx = HandleGap.toPx()
                    drawTrack(path, 0f, handleStart - gapPx, activeColor, roundStart = true)
                    val inactiveStart = handleEnd + gapPx
                    drawTrack(path, inactiveStart, size.width, inactiveColor, roundStart = false)
                    drawHandle(path, handleStart, handlePx, activeColor)
                }
            }
    )
}

/** White on the box build, black while the row behind it is focused and white. */
@Composable
private fun sliderActiveColor(focused: Boolean): Color = when {
    !isExpressiveBuild -> MoviesTheme.colors.Secondary
    focused -> ExpressiveColors.onFocus
    else -> ExpressiveColors.focus
}

@Composable
private fun sliderInactiveColor(focused: Boolean, contentAlpha: Float): Color = when {
    !isExpressiveBuild -> MoviesTheme.colors.SurfaceVariant.copy(alpha = contentAlpha)
    else -> sliderActiveColor(focused).copy(alpha = INACTIVE_TRACK_ALPHA * contentAlpha)
}

private fun DrawScope.drawTrack(
    path: Path,
    start: Float,
    end: Float,
    color: Color,
    roundStart: Boolean
) {
    if (end <= start) return
    val top = (size.height - TrackThickness.toPx()) / 2
    val outer = CornerRadius(TrackThickness.toPx() / 2)
    val inner = CornerRadius(TrackInnerCorner.toPx())
    val startCorner = if (roundStart) outer else inner
    val endCorner = if (roundStart) inner else outer
    drawSegment(path, start, end, top, size.height - top, startCorner, endCorner, color)
}

private fun DrawScope.drawHandle(path: Path, start: Float, width: Float, color: Color) {
    val corner = CornerRadius(width / 2)
    drawSegment(path, start, start + width, 0f, size.height, corner, corner, color)
}

/** Draws a horizontal span given in reading order, mirrored for right-to-left layouts. */
private fun DrawScope.drawSegment(
    path: Path,
    start: Float,
    end: Float,
    top: Float,
    bottom: Float,
    startCorner: CornerRadius,
    endCorner: CornerRadius,
    color: Color
) {
    val rtl = layoutDirection == LayoutDirection.Rtl
    val left = if (rtl) size.width - end else start
    val right = if (rtl) size.width - start else end
    val leftCorner = if (rtl) endCorner else startCorner
    val rightCorner = if (rtl) startCorner else endCorner
    path.reset()
    path.addRoundRect(
        RoundRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            topLeftCornerRadius = leftCorner,
            topRightCornerRadius = rightCorner,
            bottomRightCornerRadius = rightCorner,
            bottomLeftCornerRadius = leftCorner
        )
    )
    drawPath(path, color)
}
