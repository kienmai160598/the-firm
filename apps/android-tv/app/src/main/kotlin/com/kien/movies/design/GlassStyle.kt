package com.kien.movies.design

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.IconButtonDefaults

/**
 * Flat stand-ins for the tvOS glass (GlassStyle.swift, spec section 2): an unfocused control is one
 * [TvColors.platter] fill (add [glassEdge] for the rim), a focused one is [TvColors.focusedFill]
 * with dark content, scale 1.05, as a capsule. No blur, one fill per control.
 */
object GlassStyle {
    /** `glassButtonStyle()`: frosted at rest, white when focused. */
    @Composable
    @ReadOnlyComposable
    fun buttonColors(): ButtonColors = ButtonDefaults.colors(
        containerColor = TvColors.platter,
        contentColor = Palette.textPrimary,
        focusedContainerColor = Palette.focusedContainer,
        focusedContentColor = Palette.focusedContent,
        pressedContainerColor = Palette.focusedContainer,
        pressedContentColor = Palette.focusedContent
    )

    /** `glassProminentButtonStyle()`: the main action, raised fill at rest. */
    @Composable
    @ReadOnlyComposable
    fun prominentButtonColors(): ButtonColors = ButtonDefaults.colors(
        containerColor = TvColors.platter,
        contentColor = Palette.textPrimary,
        focusedContainerColor = Palette.focusedContainer,
        focusedContentColor = Palette.focusedContent,
        pressedContainerColor = Palette.focusedContainer,
        pressedContentColor = Palette.focusedContent
    )

    fun buttonScale(): ButtonScale = ButtonDefaults.scale(focusedScale = Focus.buttonScale)

    fun buttonShape(): ButtonShape = ButtonDefaults.shape(shape = TvShapes.pill)

    /** `glassCircleButtonStyle()`: pass to tv-material `IconButton`. */
    @Composable
    @ReadOnlyComposable
    fun circleButtonColors() = IconButtonDefaults.colors(
        containerColor = TvColors.platter,
        contentColor = Palette.textPrimary,
        focusedContainerColor = Palette.focusedContainer,
        focusedContentColor = Palette.focusedContent,
        pressedContainerColor = Palette.focusedContainer,
        pressedContentColor = Palette.focusedContent
    )

    fun circleButtonShape() = IconButtonDefaults.shape(shape = CircleShape)

    /** Dark panel fill over video or art (`darkGlassPanel`): one translucent layer. */
    val darkPanel = Color.Black.copy(alpha = 0.6f)
}

/** `glassPanel(cornerRadius:)`: a raised frosted fill. */
fun Modifier.glassPanel(cornerRadius: Dp = PANEL_CORNER): Modifier = background(Palette.frostRaised, RoundedCornerShape(cornerRadius))

/** `darkGlassPanel(cornerRadius:)`. */
fun Modifier.darkGlassPanel(cornerRadius: Dp = PANEL_CORNER): Modifier = background(GlassStyle.darkPanel, RoundedCornerShape(cornerRadius))

// Panel corner of the spec (tvOS 24 pt), as in [TvShapes.panel].
private val PANEL_CORNER = 12.dp
