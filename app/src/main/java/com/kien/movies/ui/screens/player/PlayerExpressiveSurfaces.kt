package com.kien.movies.ui.screens.player

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveMotion
import com.kien.movies.ui.theme.NuvioTheme
import com.kien.movies.ui.theme.isExpressiveBuild

private const val FLOATING_CONTAINER_ALPHA = 0.92f
private const val SIDE_PANEL_ALPHA = 0.94f
private val SIDE_PANEL_CORNER = 20.dp
private val ProgressGap = 4.dp
private val ProgressInactiveColor = Color.White.copy(alpha = 0.24f)
private val UpstreamProgressTrackColor = Color.White.copy(alpha = 0.2f)
private val UpstreamProgressFillColor = Color.White.copy(alpha = 0.85f)

/**
 * Material 3 Expressive surfaces that float over the playing video: toasts, indicators and the
 * small cards of the player. They sit on the tonal surface container instead of a black scrim
 * and take the rounder M3E corners. Other builds keep the look each call site passes in.
 */
internal object PlayerSurfaceStyle {
    @Composable
    fun floatingContainer(upstream: Color): Color = if (isExpressiveBuild) {
        NuvioTheme.colors.Surface.copy(alpha = FLOATING_CONTAINER_ALPHA)
    } else {
        upstream
    }

    /** A one-line indicator becomes a full pill. */
    fun pillShape(upstream: Shape): Shape = if (isExpressiveBuild) CircleShape else upstream

    /** A card or panel of several lines takes the 20dp panel corner. */
    @Composable
    fun cardShape(upstream: Shape): Shape = if (isExpressiveBuild) {
        RoundedCornerShape(SIDE_PANEL_CORNER)
    } else {
        upstream
    }

    /** A small card of a few short lines, where the extra large corner would cut into text. */
    @Composable
    fun compactShape(upstream: Shape): Shape = if (isExpressiveBuild) {
        RoundedCornerShape(NuvioTheme.radii.lg)
    } else {
        upstream
    }

    /** A panel docked to the right edge: rounded on its open side only. */
    @Composable
    fun sidePanelShape(): Shape {
        val corner = if (isExpressiveBuild) SIDE_PANEL_CORNER else NuvioTheme.radii.xxl
        return RoundedCornerShape(topStart = corner, bottomStart = corner)
    }

    /** A dark translucent panel the video still tints through, never a real blur. */
    @Composable
    fun sidePanelColor(): Color = if (isExpressiveBuild) {
        NuvioTheme.colors.Surface.copy(alpha = SIDE_PANEL_ALPHA)
    } else {
        NuvioTheme.colors.BackgroundElevated
    }

    /** Inside a panel docked to the right edge, its content keeps the TV safe margins. */
    @Composable
    fun sidePanelPadding(): PaddingValues {
        val inner = NuvioTheme.spacing.xl
        if (!isExpressiveBuild) return PaddingValues(inner)

        val edge = PlayerTenFoot.atLeast(inner, PlayerTenFoot.edgeMargin)
        return PaddingValues(
            start = inner,
            top = edge,
            end = PlayerTenFoot.atLeast(inner, PlayerTenFoot.sideMargin),
            bottom = edge
        )
    }

    /** The small badge of an icon inside an indicator: the accent's tonal container. */
    @Composable
    fun iconBadgeColor(upstream: Color): Color =
        if (isExpressiveBuild) NuvioTheme.colors.SecondaryVariant else upstream

    @Composable
    fun iconBadgeContent(upstream: Color): Color =
        if (isExpressiveBuild) NuvioTheme.colors.OnSecondaryVariant else upstream

    @Composable
    fun errorText(upstream: Color): Color =
        if (isExpressiveBuild) NuvioTheme.colors.Error else upstream

    /** Netflix red marks what has played; other builds keep their accent. */
    @Composable
    fun playedColor(): Color =
        if (isExpressiveBuild) ExpressiveColors.accentRed else NuvioTheme.colors.Secondary

    /** M3E linear indicators keep a track thick enough to read from the sofa. */
    fun progressThickness(upstream: Dp): Dp =
        if (isExpressiveBuild) maxOf(upstream, EXPRESSIVE_PROGRESS_THICKNESS) else upstream
}

private val EXPRESSIVE_PROGRESS_THICKNESS = 4.dp

/**
 * Enter and exit motion of player overlays: M3E springs on expressive builds, the tween each
 * call site had elsewhere. Springs run only while an overlay appears or leaves.
 */
internal object PlayerMotion {
    /** Opacity and colour. */
    fun <T> effects(upstreamMillis: Int): FiniteAnimationSpec<T> =
        if (isExpressiveBuild) ExpressiveMotion.defaultEffects() else tween(upstreamMillis)

    /** Large surfaces that slide, without overshoot. */
    fun <T> spatial(upstreamMillis: Int): FiniteAnimationSpec<T> =
        if (isExpressiveBuild) ExpressiveMotion.standardDefaultSpatial() else tween(upstreamMillis)

    /** Small parts that pop in with a little overshoot. */
    fun <T> pop(upstreamMillis: Int): FiniteAnimationSpec<T> =
        if (isExpressiveBuild) ExpressiveMotion.expressiveFastSpatial() else tween(upstreamMillis)
}

/**
 * A determinate linear progress track of loading and buffering. Expressive builds draw a plain
 * white capsule, a small gap, then the faint inactive track. Other builds draw the thin white
 * bar the player had. [progress] is read only while drawing, so a moving value costs no
 * recomposition.
 */
@Composable
internal fun Modifier.playerLinearProgress(
    progress: () -> Float,
    upstreamCorner: Dp = NuvioTheme.radii.xxs
): Modifier {
    val activeColor = if (isExpressiveBuild) Color.White else NuvioTheme.colors.Secondary

    return drawBehind {
        val fraction = progress().coerceIn(0f, 1f)
        if (isExpressiveBuild) {
            drawExpressiveProgress(fraction, activeColor)
        } else {
            drawUpstreamProgress(fraction, upstreamCorner.toPx())
        }
    }
}

private fun DrawScope.drawExpressiveProgress(fraction: Float, activeColor: Color) {
    val activeEnd = size.width * fraction
    val inactiveStart = if (fraction > 0f) activeEnd + ProgressGap.toPx() else 0f
    val corner = CornerRadius(size.height / 2f)

    drawRoundedSegment(ProgressInactiveColor, inactiveStart, size.width, corner)
    drawRoundedSegment(activeColor, 0f, activeEnd, corner)
}

private fun DrawScope.drawUpstreamProgress(fraction: Float, cornerPx: Float) {
    val corner = CornerRadius(cornerPx)

    drawRoundedSegment(UpstreamProgressTrackColor, 0f, size.width, corner)
    drawRoundedSegment(UpstreamProgressFillColor, 0f, size.width * fraction, corner)
}

private fun DrawScope.drawRoundedSegment(
    color: Color,
    start: Float,
    end: Float,
    corner: CornerRadius
) {
    if (end <= start) return

    drawRoundRect(
        color = color,
        topLeft = Offset(start, 0f),
        size = Size(end - start, size.height),
        cornerRadius = corner
    )
}

/** Grows to [fraction] of [full] height, read while measuring so the growth recomposes nothing. */
internal fun Modifier.growingHeight(full: Dp, fraction: () -> Float): Modifier =
    layout { measurable, constraints ->
        val height = (full * fraction().coerceIn(0f, 1f)).roundToPx()
        val placeable = measurable.measure(
            constraints.copy(minHeight = height, maxHeight = height)
        )
        layout(placeable.width, height) { placeable.place(0, 0) }
    }
