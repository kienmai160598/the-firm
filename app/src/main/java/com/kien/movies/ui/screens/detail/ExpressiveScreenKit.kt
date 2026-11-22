@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.detail

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveMotion
import com.kien.movies.ui.theme.MoviesTheme

// Shared by the detail, stream, search, library, profile, party, cast and ambient screens. Every
// build has the Material 3 Expressive look; the helpers that still take an upstream value ignore
// it and stay only until their last callers move to the expressive value or the M3E kit.

/** Always [expressive]: kept for callers not yet moved off it. */
@Suppress("UNUSED_PARAMETER")
internal fun <T> forBuild(upstream: T, expressive: T): T = expressive

/** Colour and opacity changes ride the M3E effects spring. */
@Suppress("UNUSED_PARAMETER")
internal fun <T> effectsSpec(upstream: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
    ExpressiveMotion.defaultEffects()

/** Large surfaces that move or resize ride the M3E standard spatial spring. */
@Suppress("UNUSED_PARAMETER")
internal fun <T> spatialSpec(upstream: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
    ExpressiveMotion.standardDefaultSpatial()

/** Small parts that move or scale ride the M3E fast spatial spring. */
@Suppress("UNUSED_PARAMETER")
internal fun <T> fastSpatialSpec(upstream: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
    ExpressiveMotion.expressiveFastSpatial()

/**
 * The box renders 960 by 540 dp and is read from across a room, so nothing on it may be
 * smaller than these.
 */
internal object TvSizeFloors {
    val metaText: TextUnit = 14.sp
    val bodyText: TextUnit = 16.sp
    val rowTitle: TextUnit = 20.sp
    val screenTitle: TextUnit = 28.sp
    val control: Dp = 48.dp
    val buttonIcon: Dp = 24.dp
}

internal fun Dp.atLeastOnBox(floor: Dp): Dp = max(this, floor)

internal fun TextUnit.atLeastOnBox(floor: TextUnit): TextUnit =
    if (this.isSpecified && this.value < floor.value) floor else this

/** Raises the font to [floor]; the line height grows by the same step. */
internal fun TextStyle.atLeastOnBox(floor: TextUnit): TextStyle {
    if (!fontSize.isSpecified || fontSize.value >= floor.value) return this

    val step = floor.value - fontSize.value
    val raisedLineHeight = if (lineHeight.isSpecified) (lineHeight.value + step).sp else lineHeight
    return copy(fontSize = floor, lineHeight = raisedLineHeight)
}

/**
 * A tonal card in a lazy row: the container fills with the focus tone on focus, without a
 * ring. Fixed colours, not animated ones, so a focus move does not recompose every item.
 */
@Composable
internal fun tonalCardColors(resting: Color): CardColors {
    val focused = MoviesTheme.colors.FocusBackground
    return CardDefaults.colors(
        containerColor = resting,
        focusedContainerColor = focused,
        pressedContainerColor = focused
    )
}

@Composable
internal fun noRingCardBorder(): CardBorder = CardDefaults.border(
    border = Border.None,
    focusedBorder = Border.None,
    pressedBorder = Border.None
)

/** A tonal dialog button: rests frosted and turns white on focus. */
@Composable
internal fun tonalButtonColors(resting: Color = ExpressiveColors.frosted): ButtonColors {
    return ButtonDefaults.colors(
        containerColor = resting,
        contentColor = MoviesTheme.colors.TextPrimary,
        focusedContainerColor = MoviesTheme.colors.Secondary,
        focusedContentColor = MoviesTheme.colors.OnSecondary,
        pressedContainerColor = MoviesTheme.colors.Secondary,
        pressedContentColor = MoviesTheme.colors.OnSecondary
    )
}

/** A tv-material Button's own label style, raised to the TV floor. */
@Composable
internal fun buttonLabelStyle(): TextStyle =
    MaterialTheme.typography.labelLarge.atLeastOnBox(TvSizeFloors.bodyText)

/**
 * A tv-material Button whose [selected] state stays tonal, the M3E selected look, so only the
 * focused one carries the accent. The upstream colours are ignored.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun selectableButtonColors(
    selected: Boolean,
    upstreamSelected: Color = Color.Unspecified,
    upstreamSelectedContent: Color = Color.Unspecified
): ButtonColors {
    return tonalButtonColors(
        if (selected) ExpressiveColors.frostedStrong else ExpressiveColors.frosted
    )
}

/** Focusable controls on the box are at least [TvSizeFloors.control] tall. */
internal fun Modifier.tvControlHeight(): Modifier = heightIn(min = TvSizeFloors.control)

// A tv-material Button's label is 22 sp tall on the box, so 13 dp above and below makes 48 dp.
private val BoxButtonPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp)

/** A tv-material Button's padding, tall enough for the TV control height. */
internal fun tvButtonPadding(): PaddingValues = BoxButtonPadding
