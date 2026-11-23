package com.kien.movies.design

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.IconButtonDefaults

/**
 * Flat stand-ins for the tvOS 26 glass (GlassStyle.swift): frosted white fills at 14, 22 and 30%
 * and a focused control in solid white with black content. No blur, one fill per control.
 */
object GlassStyle {
    /** `glassButtonStyle()`: frosted at rest, white when focused. */
    @Composable
    @ReadOnlyComposable
    fun buttonColors(): ButtonColors = ButtonDefaults.colors(
        containerColor = Palette.frostRest,
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
        containerColor = Palette.frostStrong,
        contentColor = Palette.textPrimary,
        focusedContainerColor = Palette.focusedContainer,
        focusedContentColor = Palette.focusedContent,
        pressedContainerColor = Palette.focusedContainer,
        pressedContentColor = Palette.focusedContent
    )

    fun buttonScale(): ButtonScale = ButtonDefaults.scale(focusedScale = Focus.buttonScale)

    fun buttonShape(): ButtonShape = ButtonDefaults.shape(shape = RoundedCornerShape(CardSize.cornerRadius))

    /** `glassCircleButtonStyle()`: pass to tv-material `IconButton`. */
    @Composable
    @ReadOnlyComposable
    fun circleButtonColors() = IconButtonDefaults.colors(
        containerColor = Palette.frostRest,
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
fun Modifier.glassPanel(cornerRadius: Dp = CardSize.cornerRadius): Modifier = background(Palette.frostRaised, RoundedCornerShape(cornerRadius))

/** `darkGlassPanel(cornerRadius:)`. */
fun Modifier.darkGlassPanel(cornerRadius: Dp = CardSize.cornerRadius): Modifier = background(GlassStyle.darkPanel, RoundedCornerShape(cornerRadius))
