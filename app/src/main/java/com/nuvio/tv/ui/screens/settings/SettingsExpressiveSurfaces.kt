@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nuvio.tv.ui.components.expressiveButtonBorder
import com.nuvio.tv.ui.components.expressiveButtonColors
import com.nuvio.tv.ui.components.expressiveButtonScale
import com.nuvio.tv.ui.components.expressiveButtonShape
import com.nuvio.tv.ui.components.expressivePrimaryButtonColors
import com.nuvio.tv.ui.theme.ExpressiveColors
import com.nuvio.tv.ui.theme.ExpressiveFocus
import com.nuvio.tv.ui.theme.ExpressiveRadii
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.theme.TvSizeFloors
import com.nuvio.tv.ui.theme.isExpressiveBuild

// Shared by the settings, account, addon, plugin and TMDB screens for the tv-material Surfaces and
// Cards that components/ExpressiveControlStyle does not cover. On the expressive build a card
// answers focus with a tonal fill, a large corner and a small grow, and a surface used as a button
// with the accent and a pill; neither draws a ring. Shapes switch per state instead of animating,
// so long lists cost no frame per item. Other builds get upstream's value back.

private const val CARD_FOCUS_SCALE = 1.02f
private val SelectedOutlineWidth = 2.dp

private val ExpressiveCardShape = RoundedCornerShape(ExpressiveRadii.forBuild.xl)
private val ExpressivePanelShape = RoundedCornerShape(ExpressiveRadii.forBuild.xxl)
private val PillShape = RoundedCornerShape(percent = ExpressiveFocus.PILL_PERCENT)

/** A card or row corner; [upstream] on other builds. */
internal fun expressiveCardShape(upstream: Shape): Shape =
    if (isExpressiveBuild) ExpressiveCardShape else upstream

/** A large container or hand-drawn dialog corner, M3's extra-large. */
internal fun expressivePanelShape(upstream: Shape): Shape =
    if (isExpressiveBuild) ExpressivePanelShape else upstream

/** The size of an icon inside a button, never under the box's 24dp floor. */
internal fun buttonIconSize(upstream: Dp): Dp =
    TvSizeFloors.atLeast(upstream, TvSizeFloors.controlIcon)

/** A focused card's fill, tonal once its ring is gone. */
@Composable
internal fun expressiveCardFocusFill(upstream: Color): Color =
    if (isExpressiveBuild) NuvioTheme.colors.FocusBackground else upstream

/** A notice drawn in a fixed blue upstream: the theme's tonal container on the box. */
@Composable
internal fun expressiveNoticeColor(upstream: Color): Color =
    if (isExpressiveBuild) NuvioTheme.colors.FocusBackground else upstream

@Composable
internal fun ClickableSurfaceBorder.expressiveRingless(): ClickableSurfaceBorder =
    if (isExpressiveBuild) {
        ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border.None,
            pressedBorder = Border.None
        )
    } else {
        this
    }

@Composable
internal fun CardBorder.expressiveRingless(): CardBorder =
    if (isExpressiveBuild) {
        CardDefaults.border(
            border = Border.None,
            focusedBorder = Border.None,
            pressedBorder = Border.None
        )
    } else {
        this
    }

@Composable
internal fun ClickableSurfaceShape.expressiveCard(): ClickableSurfaceShape =
    if (isExpressiveBuild) ClickableSurfaceDefaults.shape(ExpressiveCardShape) else this

@Composable
internal fun ClickableSurfaceScale.expressiveCardGrow(): ClickableSurfaceScale =
    if (isExpressiveBuild) ClickableSurfaceDefaults.scale(focusedScale = CARD_FOCUS_SCALE) else this

@Composable
internal fun CardScale.expressiveCardGrow(): CardScale =
    if (isExpressiveBuild) CardDefaults.scale(focusedScale = CARD_FOCUS_SCALE) else this

/** A surface used as a button or chip: a pill that squares its corners a little on focus. */
@Composable
internal fun ClickableSurfaceShape.expressivePill(): ClickableSurfaceShape =
    if (isExpressiveBuild) {
        ClickableSurfaceDefaults.shape(
            shape = PillShape,
            focusedShape = PillShape,
            pressedShape = PillShape
        )
    } else {
        this
    }

/** A surface used as a button or chip grows like the other expressive controls. */
@Composable
internal fun ClickableSurfaceScale.expressiveControlGrow(): ClickableSurfaceScale =
    if (isExpressiveBuild) {
        ClickableSurfaceDefaults.scale(focusedScale = ExpressiveFocus.CONTROL_SCALE)
    } else {
        this
    }

/**
 * A surface used as a button or chip: on a card surface, tonal while [selected] or the main
 * action ([primary]), filled with the accent on focus.
 */
@Composable
internal fun ClickableSurfaceColors.expressiveControl(
    selected: Boolean = false,
    primary: Boolean = false
): ClickableSurfaceColors {
    if (!isExpressiveBuild) return this
    return ClickableSurfaceDefaults.colors(
        containerColor = controlRestingFill(selected, primary),
        contentColor = ExpressiveColors.textPrimary,
        focusedContainerColor = ExpressiveColors.focus,
        focusedContentColor = ExpressiveColors.onFocus,
        pressedContainerColor = ExpressiveColors.secondaryPressed,
        pressedContentColor = ExpressiveColors.onFocus
    )
}

/**
 * A control at rest on the box build: frosted, brighter for the main action or a chosen option,
 * so only the focused control is solid white.
 */
private fun controlRestingFill(selected: Boolean, primary: Boolean): Color =
    if (primary || selected) ExpressiveColors.frostedStrong else ExpressiveColors.frosted

/** The content colour inside an expressive control, for text and icons that set one. */
@Composable
internal fun expressiveControlContent(focused: Boolean, upstream: Color): Color =
    if (!isExpressiveBuild) {
        upstream
    } else if (focused) {
        NuvioTheme.colors.OnSecondary
    } else {
        NuvioTheme.colors.TextPrimary
    }

@Composable
internal fun CardShape.expressiveCard(): CardShape =
    if (isExpressiveBuild) CardDefaults.shape(ExpressiveCardShape) else this

/** A Card used as a button or chip: a pill that squares its corners a little on focus. */
@Composable
internal fun CardShape.expressivePill(): CardShape =
    if (isExpressiveBuild) {
        CardDefaults.shape(
            shape = PillShape,
            focusedShape = PillShape,
            pressedShape = PillShape
        )
    } else {
        this
    }

@Composable
internal fun CardScale.expressiveControlGrow(): CardScale =
    if (isExpressiveBuild) {
        CardDefaults.scale(focusedScale = ExpressiveFocus.CONTROL_SCALE)
    } else {
        this
    }

/** A Card used as a button or chip, coloured like [ClickableSurfaceColors.expressiveControl]. */
@Composable
internal fun CardColors.expressiveControl(
    selected: Boolean = false,
    primary: Boolean = false
): CardColors {
    if (!isExpressiveBuild) return this
    return CardDefaults.colors(
        containerColor = controlRestingFill(selected, primary),
        contentColor = ExpressiveColors.textPrimary,
        focusedContainerColor = ExpressiveColors.focus,
        focusedContentColor = ExpressiveColors.onFocus,
        pressedContainerColor = ExpressiveColors.secondaryPressed,
        pressedContentColor = ExpressiveColors.onFocus
    )
}

/** A tv Button in the expressive look; [primary] rests on the accent's container. */
@Composable
internal fun ButtonColors.expressiveControl(primary: Boolean = false): ButtonColors =
    if (primary) expressivePrimaryButtonColors(this) else expressiveButtonColors(this)

/** A tv Button that marks a choice: tonal while [selected], accent on focus. */
@Composable
internal fun ButtonColors.expressiveChoice(selected: Boolean): ButtonColors =
    expressiveButtonColors(this, selected)

internal fun ButtonShape.expressivePill(): ButtonShape = expressiveButtonShape(this)

@Composable
internal fun ButtonBorder.expressiveRingless(): ButtonBorder = expressiveButtonBorder(this)

internal fun ButtonScale.expressiveControlGrow(): ButtonScale = expressiveButtonScale(this)

/**
 * Text or an icon inside an expressive control follows the control's content colour, which turns
 * to on-accent while it is focused; a fixed colour would vanish on the accent fill.
 */
@Composable
@ReadOnlyComposable
internal fun onControlColor(upstream: Color): Color =
    if (isExpressiveBuild) LocalContentColor.current else upstream

/** A plain tile drawn in a fixed grey upstream: the theme's highest surface container. */
@Composable
@ReadOnlyComposable
internal fun expressiveSurfaceOr(upstream: Color): Color =
    if (isExpressiveBuild) NuvioTheme.colors.SurfaceVariant else upstream

@Composable
@ReadOnlyComposable
internal fun expressiveErrorOr(upstream: Color): Color =
    if (isExpressiveBuild) NuvioTheme.colors.Error else upstream

@Composable
@ReadOnlyComposable
internal fun expressiveWarningOr(upstream: Color): Color =
    if (isExpressiveBuild) NuvioTheme.colors.Warning else upstream

/**
 * A choice card drops its focus ring on the box but keeps its selection readable: an accent
 * outline while [selected], whether focused or not, as M3's selected outlined card.
 */
@Composable
internal fun CardBorder.expressiveSelection(selected: Boolean): CardBorder {
    if (!isExpressiveBuild) return this
    val outline = if (selected) {
        Border(
            border = BorderStroke(SelectedOutlineWidth, NuvioTheme.colors.Secondary),
            shape = ExpressiveCardShape
        )
    } else {
        Border.None
    }
    return CardDefaults.border(border = outline, focusedBorder = outline, pressedBorder = outline)
}
