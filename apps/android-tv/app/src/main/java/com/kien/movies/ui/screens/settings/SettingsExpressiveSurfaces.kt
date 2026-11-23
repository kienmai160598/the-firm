@file:OptIn(ExperimentalTvMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.kien.movies.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale
import androidx.tv.material3.CardShape
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.ClickableSurfaceShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.components.expressive.moviesButtonBorder
import com.kien.movies.ui.components.expressive.moviesButtonColors
import com.kien.movies.ui.components.expressive.moviesButtonScale
import com.kien.movies.ui.components.expressive.moviesButtonShape
import com.kien.movies.ui.components.expressive.moviesCardBorder
import com.kien.movies.ui.components.expressive.moviesCardColors
import com.kien.movies.ui.components.expressive.moviesCardScale
import com.kien.movies.ui.components.expressive.moviesSurfaceBorder
import com.kien.movies.ui.components.expressive.moviesSurfaceColors
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.TvSizeFloors

// Shared by the settings, account, addon, plugin and TMDB screens for the tv-material Surfaces,
// Cards and Buttons they still draw themselves. Each helper hands back the M3E kit's value
// (components/expressive), so a control styled through it looks like a kit control: 12dp cards,
// capsule buttons and chips, frosted at rest, white on focus, no ring. The receiver or `upstream`
// argument a call site passes is not read.

private val SelectedOutlineWidth = 2.dp

@Composable
private fun surfaceShapeInEveryState(shape: Shape): ClickableSurfaceShape = ClickableSurfaceDefaults.shape(
    shape = shape,
    focusedShape = shape,
    pressedShape = shape,
    disabledShape = shape,
    focusedDisabledShape = shape
)

private val ControlSurfaceScale: ClickableSurfaceScale =
    ClickableSurfaceDefaults.scale(focusedScale = ExpressiveFocus.CONTROL_SCALE, pressedScale = 1f)

private val CapsuleCardShape: CardShape = CardDefaults.shape(
    shape = M3eShapes.capsule,
    focusedShape = M3eShapes.capsule,
    pressedShape = M3eShapes.capsule
)

private val TextCardShape: CardShape = CardDefaults.shape(shape = M3eShapes.card)

/** The size of an icon inside a button, never under the box's 24dp floor. */
internal fun buttonIconSize(upstream: Dp): Dp = TvSizeFloors.atLeast(upstream, TvSizeFloors.controlIcon)

@Composable
internal fun ClickableSurfaceBorder.expressiveRingless(): ClickableSurfaceBorder = moviesSurfaceBorder()

@Composable
internal fun CardBorder.expressiveRingless(): CardBorder = moviesCardBorder()

@Composable
internal fun ClickableSurfaceShape.expressiveCard(): ClickableSurfaceShape = surfaceShapeInEveryState(M3eShapes.card)

@Composable
internal fun ClickableSurfaceScale.expressiveCardGrow(): ClickableSurfaceScale = ControlSurfaceScale

@Composable
internal fun CardScale.expressiveCardGrow(): CardScale = moviesCardScale

/** A surface used as a button or chip: a capsule in every state. */
@Composable
internal fun ClickableSurfaceShape.expressivePill(): ClickableSurfaceShape = surfaceShapeInEveryState(M3eShapes.capsule)

/** A surface used as a button or chip grows on the kit's control scale. */
@Composable
internal fun ClickableSurfaceScale.expressiveControlGrow(): ClickableSurfaceScale = ControlSurfaceScale

/**
 * A surface used as a button or chip: frosted at rest, brighter while [selected] or the main
 * action ([primary]), solid white with black content on focus.
 */
@Composable
internal fun ClickableSurfaceColors.expressiveControl(
    selected: Boolean = false,
    primary: Boolean = false
): ClickableSurfaceColors = moviesSurfaceColors(selected = selected || primary)

@Composable
internal fun CardShape.expressiveCard(): CardShape = TextCardShape

/** A Card used as a button or chip: a capsule in every state. */
@Composable
internal fun CardShape.expressivePill(): CardShape = CapsuleCardShape

@Composable
internal fun CardScale.expressiveControlGrow(): CardScale = moviesCardScale

/** A Card used as a button or chip, coloured like [ClickableSurfaceColors.expressiveControl]. */
@Composable
internal fun CardColors.expressiveControl(
    selected: Boolean = false,
    primary: Boolean = false
): CardColors = moviesCardColors(selected = selected || primary)

/** A tv Button in the kit's look; [primary] rests on the brighter frosted fill. */
@Composable
internal fun ButtonColors.expressiveControl(primary: Boolean = false): ButtonColors = moviesButtonColors(if (primary) MoviesButtonStyle.Primary else MoviesButtonStyle.Secondary)

/** A tv Button that marks a choice: brighter frosted while [selected], white on focus. */
@Composable
internal fun ButtonColors.expressiveChoice(selected: Boolean): ButtonColors = moviesButtonColors(if (selected) MoviesButtonStyle.Selected else MoviesButtonStyle.Secondary)

internal fun ButtonShape.expressivePill(): ButtonShape = moviesButtonShape

@Composable
internal fun ButtonBorder.expressiveRingless(): ButtonBorder = moviesButtonBorder()

internal fun ButtonScale.expressiveControlGrow(): ButtonScale = moviesButtonScale

/**
 * A choice card has no focus ring but keeps its selection readable: an accent outline while
 * [selected], whether focused or not, as M3's selected outlined card.
 */
@Composable
internal fun CardBorder.expressiveSelection(selected: Boolean): CardBorder {
    val outline = if (selected) {
        Border(
            border = BorderStroke(SelectedOutlineWidth, MoviesTheme.colors.Secondary),
            shape = M3eShapes.card
        )
    } else {
        Border.None
    }
    return CardDefaults.border(border = outline, focusedBorder = outline, pressedBorder = outline)
}
