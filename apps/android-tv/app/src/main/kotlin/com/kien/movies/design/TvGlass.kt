package com.kien.movies.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.ClickableSurfaceShape
import androidx.tv.material3.IconButtonDefaults

// Flat stand-ins for the tvOS glass (GlassStyle.swift, spec section 2): an unfocused control is one
// [TvColors.platter] fill with the glass edge, a focused one is [TvColors.focusedFill] with dark
// content, scale 1.05. No blur, one fill per control.

/**
 * The glass look as tv-material parameters: platter fill at rest, solid white with dark content
 * when focused or pressed, and the glass edge only while not focused.
 */
object TvGlass {
    private val edge = glassEdgeBorder(RectangleShape)

    @Composable
    @ReadOnlyComposable
    fun buttonColors(): ButtonColors = ButtonDefaults.colors(
        containerColor = TvColors.platter,
        contentColor = TvColors.textPrimary,
        focusedContainerColor = TvColors.focusedFill,
        focusedContentColor = TvColors.focusedContent,
        pressedContainerColor = TvColors.focusedFill,
        pressedContentColor = TvColors.focusedContent,
        disabledContainerColor = TvColors.platter.copy(alpha = DISABLED_PLATTER_ALPHA),
        disabledContentColor = TvColors.textTertiary
    )

    fun pillShape(): ButtonShape = ButtonDefaults.shape(shape = TvShapes.pill)

    /** A pill's side padding: where its label starts, and how far the pill hangs past its column. */
    val pillPaddingHorizontal = 16.dp

    /** Horizontal only: a pill is [TvControls.pillHeight] tall, the label centres in it. */
    val pillPadding = PaddingValues(horizontal = pillPaddingHorizontal)

    fun buttonScale(): ButtonScale = ButtonDefaults.scale(focusedScale = Focus.buttonScale)

    @Composable
    @ReadOnlyComposable
    fun buttonBorder(): ButtonBorder = ButtonDefaults.border(
        border = edge.withShape(TvShapes.pill),
        focusedBorder = Border.None,
        pressedBorder = Border.None,
        disabledBorder = edge.withShape(TvShapes.pill),
        focusedDisabledBorder = edge.withShape(TvShapes.pill)
    )

    @Composable
    @ReadOnlyComposable
    fun surfaceColors(containerColor: Color = TvColors.platter): ClickableSurfaceColors = ClickableSurfaceDefaults.colors(
        containerColor = containerColor,
        contentColor = TvColors.textPrimary,
        focusedContainerColor = TvColors.focusedFill,
        focusedContentColor = TvColors.focusedContent,
        pressedContainerColor = TvColors.focusedFill,
        pressedContentColor = TvColors.focusedContent
    )

    @Composable
    @ReadOnlyComposable
    fun surfaceShape(shape: Shape): ClickableSurfaceShape = ClickableSurfaceDefaults.shape(shape = shape)

    fun surfaceScale(focusedScale: Float = Focus.buttonScale): ClickableSurfaceScale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale)

    @Composable
    @ReadOnlyComposable
    fun surfaceBorder(shape: Shape): ClickableSurfaceBorder = ClickableSurfaceDefaults.border(
        border = edge.withShape(shape),
        focusedBorder = Border.None,
        pressedBorder = Border.None
    )

    /** The rim of a circular tv-material `IconButton` over video: [liquidGlassRim]. */
    @Composable
    @ReadOnlyComposable
    fun liquidCircleBorder(): ButtonBorder = IconButtonDefaults.border(
        border = liquidGlassBorder(CircleShape),
        focusedBorder = Border.None,
        pressedBorder = Border.None,
        disabledBorder = liquidGlassBorder(CircleShape),
        focusedDisabledBorder = liquidGlassBorder(CircleShape)
    )

    /**
     * A circular tv-material `IconButton` as glass: [containerColor] at rest (the platter, or a
     * brighter fill for a control whose menu is open), white with dark content when focused.
     */
    @Composable
    @ReadOnlyComposable
    fun circleColors(containerColor: Color = TvColors.platter): ButtonColors = IconButtonDefaults.colors(
        containerColor = containerColor,
        contentColor = TvColors.textPrimary,
        focusedContainerColor = TvColors.focusedFill,
        focusedContentColor = TvColors.focusedContent,
        pressedContainerColor = TvColors.focusedFill,
        pressedContentColor = TvColors.focusedContent,
        disabledContainerColor = TvColors.platter,
        disabledContentColor = TvColors.textTertiary
    )

    fun circleShape(): ButtonShape = IconButtonDefaults.shape(shape = CircleShape)

    fun circleScale(): ButtonScale = IconButtonDefaults.scale(focusedScale = Focus.buttonScale)

    private fun Border.withShape(shape: Shape): Border = copy(shape = shape)
}

/** Dark panel fill over video or art (`darkGlassPanel` on tvOS): one translucent layer. */
fun Modifier.darkGlassPanel(cornerRadius: Dp = PANEL_CORNER): Modifier = background(DARK_PANEL, RoundedCornerShape(cornerRadius))

private val DARK_PANEL = Color.Black.copy(alpha = 0.6f)

// Panel corner of the spec (tvOS 24 pt), as in [TvShapes.panel].
private val PANEL_CORNER = 12.dp
private const val DISABLED_PLATTER_ALPHA = 0.5f
