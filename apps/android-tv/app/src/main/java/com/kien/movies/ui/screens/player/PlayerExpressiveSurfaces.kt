package com.kien.movies.ui.screens.player

import androidx.compose.animation.core.FiniteAnimationSpec
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
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveMotion
import com.kien.movies.ui.theme.MoviesTheme

private const val FLOATING_CONTAINER_ALPHA = 0.92f
private const val SIDE_PANEL_ALPHA = 0.94f
private val SIDE_PANEL_CORNER = 20.dp
private val ProgressGap = 4.dp
private val ProgressInactiveColor = Color.White.copy(alpha = 0.24f)

/**
 * Material 3 Expressive surfaces that float over the playing video: toasts, indicators and the
 * small cards of the player. They sit on the tonal surface container instead of a black scrim
 * and take the rounder M3E corners.
 */
internal object PlayerSurfaceStyle {
    @Composable
    fun floatingContainer(): Color = MoviesTheme.colors.Surface.copy(alpha = FLOATING_CONTAINER_ALPHA)

    /** A one-line indicator becomes a full pill. */
    fun pillShape(): Shape = CircleShape

    /** A card or panel of several lines takes the 20dp panel corner. */
    fun cardShape(): Shape = M3eShapes.panel

    /** A small card of a few short lines, where the extra large corner would cut into text. */
    @Composable
    fun compactShape(): Shape = RoundedCornerShape(MoviesTheme.radii.lg)

    /** A panel docked to the right edge: rounded on its open side only. */
    fun sidePanelShape(): Shape = RoundedCornerShape(
        topStart = SIDE_PANEL_CORNER,
        bottomStart = SIDE_PANEL_CORNER
    )

    /** A dark translucent panel the video still tints through, never a real blur. */
    @Composable
    fun sidePanelColor(): Color = MoviesTheme.colors.Surface.copy(alpha = SIDE_PANEL_ALPHA)

    /** Inside a panel docked to the right edge, its content keeps the TV safe margins. */
    @Composable
    fun sidePanelPadding(): PaddingValues {
        val inner = MoviesTheme.spacing.xl
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
    fun iconBadgeColor(): Color = MoviesTheme.colors.SecondaryVariant

    @Composable
    fun iconBadgeContent(): Color = MoviesTheme.colors.OnSecondaryVariant

    @Composable
    fun errorText(): Color = MoviesTheme.colors.Error

    /** Netflix red marks what has played. */
    fun playedColor(): Color = ExpressiveColors.accentRed

    /** M3E linear indicators keep a track thick enough to read from the sofa. */
    fun progressThickness(size: Dp): Dp = maxOf(size, EXPRESSIVE_PROGRESS_THICKNESS)
}

private val EXPRESSIVE_PROGRESS_THICKNESS = 4.dp

/** Enter and exit motion of player overlays: M3E springs that run only while one moves. */
internal object PlayerMotion {
    /** Opacity and colour. */
    fun <T> effects(): FiniteAnimationSpec<T> = ExpressiveMotion.defaultEffects()

    /** Large surfaces that slide, without overshoot. */
    fun <T> spatial(): FiniteAnimationSpec<T> = ExpressiveMotion.standardDefaultSpatial()

    /** Small parts that pop in with a little overshoot. */
    fun <T> pop(): FiniteAnimationSpec<T> = ExpressiveMotion.expressiveFastSpatial()
}

/**
 * A determinate linear progress track of loading and buffering: a plain white capsule, a small
 * gap, then the faint inactive track. [progress] is read only while drawing, so a moving value
 * costs no recomposition.
 */
internal fun Modifier.playerLinearProgress(progress: () -> Float): Modifier = drawBehind {
    val fraction = progress().coerceIn(0f, 1f)
    drawExpressiveProgress(fraction, Color.White)
}

private fun DrawScope.drawExpressiveProgress(fraction: Float, activeColor: Color) {
    val activeEnd = size.width * fraction
    val inactiveStart = if (fraction > 0f) activeEnd + ProgressGap.toPx() else 0f
    val corner = CornerRadius(size.height / 2f)

    drawRoundedSegment(ProgressInactiveColor, inactiveStart, size.width, corner)
    drawRoundedSegment(activeColor, 0f, activeEnd, corner)
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
internal fun Modifier.growingHeight(full: Dp, fraction: () -> Float): Modifier = layout { measurable, constraints ->
    val height = (full * fraction().coerceIn(0f, 1f)).roundToPx()
    val placeable = measurable.measure(
        constraints.copy(minHeight = height, maxHeight = height)
    )
    layout(placeable.width, height) { placeable.place(0, 0) }
}
