package com.kien.movies.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardGlow
import androidx.tv.material3.CardScale
import androidx.tv.material3.CardShape as TvCardShape
import androidx.tv.material3.Glow

/**
 * The focus look of every card, as parameters for tv-material's `Card`: scale 1.1 drawn by the
 * card's surface in a graphics layer (the card never changes layout size), one soft shadow and a
 * thin light rim ([focusedCardRim]), no ring. Use all five together:
 *
 * ```
 * Card(
 *     onClick = ...,
 *     shape = CardFocus.shape(),
 *     colors = CardFocus.colors(),
 *     scale = CardFocus.scale(),
 *     border = CardFocus.border(),
 *     glow = CardFocus.glow(),
 * ) { ArtworkImage(...) }
 * ```
 *
 * The shadow is a tv-material Glow (spec: one soft shadow, 12 dp, black 40%); its cost on the box
 * is not measured yet.
 */
object CardFocus {
    fun shape(): TvCardShape = CardDefaults.shape(shape = CardShape.shape)

    @Composable
    @ReadOnlyComposable
    fun colors(): CardColors = CardDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = Palette.textPrimary,
        focusedContainerColor = Color.Transparent,
        focusedContentColor = Palette.textPrimary
    )

    fun scale(): CardScale = CardDefaults.scale(focusedScale = Focus.cardScale)

    @Composable
    @ReadOnlyComposable
    fun border(): CardBorder = CardDefaults.border(
        border = Border.None,
        focusedBorder = Border(border = BorderStroke(GLASS_EDGE_WIDTH, focusedCardRim), shape = CardShape.shape)
    )

    fun glow(): CardGlow = CardDefaults.glow(
        focusedGlow = Glow(elevationColor = TvColors.focusShadow, elevation = Focus.shadowElevation)
    )
}
