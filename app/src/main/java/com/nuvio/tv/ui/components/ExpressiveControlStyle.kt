@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale
import androidx.tv.material3.CardShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.LocalTextStyle
import com.nuvio.tv.ui.theme.ExpressiveColors
import com.nuvio.tv.ui.theme.ExpressiveFocus
import com.nuvio.tv.ui.theme.ExpressiveRadii
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.theme.bodyFloor
import com.nuvio.tv.ui.theme.isExpressiveBuild

// The Apple TV look for tv-material Buttons and Cards on the box build: frosted capsules at rest,
// solid white with black content on focus, no outline rings. Each helper takes the value the
// call site used before and returns it unchanged on other builds, so upstream keeps its look.
// Shapes switch per state instead of animating, so a list of them costs no frame per item.

private val PillShape = RoundedCornerShape(percent = ExpressiveFocus.PILL_PERCENT)
private val FocusedPillShape = RoundedCornerShape(percent = ExpressiveFocus.FOCUSED_PILL_PERCENT)

/** The corner of a text card, list row or media card on the box build: 12dp. */
internal val ExpressiveOptionShape: Shape = RoundedCornerShape(ExpressiveRadii.forBuild.md)

/** The corner of a panel or dialog: 20dp. */
internal val ExpressiveContainerShape: Shape = RoundedCornerShape(ExpressiveRadii.forBuild.xl)

/** A capsule in every state, as tvOS draws its buttons. */
internal fun expressiveButtonShape(upstream: ButtonShape): ButtonShape =
    if (!isExpressiveBuild) {
        upstream
    } else {
        ButtonDefaults.shape(
            shape = PillShape,
            focusedShape = FocusedPillShape,
            pressedShape = FocusedPillShape
        )
    }

/** No outline in any state: focus reads from the white fill, selection from a frosted one. */
@Composable
internal fun expressiveButtonBorder(upstream: ButtonBorder): ButtonBorder =
    if (!isExpressiveBuild) {
        upstream
    } else {
        ButtonDefaults.border(
            border = Border.None,
            focusedBorder = Border.None,
            pressedBorder = Border.None,
            disabledBorder = Border.None,
            focusedDisabledBorder = Border.None
        )
    }

/** Grows a focused button as much as the other expressive controls. */
internal fun expressiveButtonScale(upstream: ButtonScale): ButtonScale =
    if (!isExpressiveBuild) {
        upstream
    } else {
        ButtonDefaults.scale(focusedScale = ExpressiveFocus.CONTROL_SCALE)
    }

/**
 * Frosted at rest, a little brighter while [selected]; solid white with black content on focus,
 * or Netflix red with white content when the button deletes something ([destructive]).
 */
@Composable
internal fun expressiveButtonColors(
    upstream: ButtonColors,
    selected: Boolean = false,
    destructive: Boolean = false
): ButtonColors {
    if (!isExpressiveBuild) return upstream
    return ButtonDefaults.colors(
        containerColor = if (selected) ExpressiveColors.frostedStrong else ExpressiveColors.frosted,
        contentColor = ExpressiveColors.textPrimary,
        focusedContainerColor = if (destructive) ExpressiveColors.accentRed else ExpressiveColors.focus,
        focusedContentColor = if (destructive) Color.White else ExpressiveColors.onFocus,
        pressedContainerColor = ExpressiveColors.secondaryPressed,
        pressedContentColor = ExpressiveColors.onFocus
    )
}

/**
 * [expressiveButtonColors] for the main action: a brighter frosted capsule at rest so it reads
 * as the default, solid white on focus.
 */
@Composable
internal fun expressivePrimaryButtonColors(upstream: ButtonColors): ButtonColors {
    if (!isExpressiveBuild) return upstream
    return ButtonDefaults.colors(
        containerColor = ExpressiveColors.frostedStrong,
        contentColor = ExpressiveColors.textPrimary,
        focusedContainerColor = ExpressiveColors.focus,
        focusedContentColor = ExpressiveColors.onFocus,
        pressedContainerColor = ExpressiveColors.secondaryPressed,
        pressedContentColor = ExpressiveColors.onFocus
    )
}

/** A text card or list row with the 12dp card corner. */
internal fun expressiveOptionCardShape(upstream: CardShape): CardShape =
    if (!isExpressiveBuild) upstream else CardDefaults.shape(shape = ExpressiveOptionShape)

/** No ring on a text card: focus reads from its fill and lift. */
@Composable
internal fun expressiveOptionCardBorder(upstream: CardBorder): CardBorder {
    if (!isExpressiveBuild) return upstream
    return CardDefaults.border(
        border = Border.None,
        focusedBorder = Border.None,
        pressedBorder = Border.None
    )
}

/**
 * A text card or row: frosted at rest, brighter while [selected]. On focus it fills solid white
 * with black content when [whiteFocus] (its text must read LocalContentColor), otherwise a
 * strong frosted fill that keeps fixed white text legible.
 */
@Composable
internal fun expressiveOptionCardColors(
    upstream: CardColors,
    selected: Boolean = false,
    whiteFocus: Boolean = false
): CardColors {
    if (!isExpressiveBuild) return upstream
    val focusedContainer =
        if (whiteFocus) ExpressiveColors.focus else ExpressiveColors.frostedFocused
    val focusedContent =
        if (whiteFocus) ExpressiveColors.onFocus else ExpressiveColors.textPrimary
    return CardDefaults.colors(
        containerColor = if (selected) ExpressiveColors.frostedStrong else ExpressiveColors.frosted,
        contentColor = ExpressiveColors.textPrimary,
        focusedContainerColor = focusedContainer,
        focusedContentColor = focusedContent,
        pressedContainerColor = focusedContainer,
        pressedContentColor = focusedContent
    )
}

/** A Card used as a dialog or form action: a capsule. */
internal fun expressiveActionCardShape(upstream: CardShape): CardShape =
    if (!isExpressiveBuild) {
        upstream
    } else {
        CardDefaults.shape(
            shape = PillShape,
            focusedShape = FocusedPillShape,
            pressedShape = FocusedPillShape
        )
    }

/** No ring on an action Card: focus reads from its fill and lift. */
@Composable
internal fun expressiveActionCardBorder(upstream: CardBorder): CardBorder {
    if (!isExpressiveBuild) return upstream
    return CardDefaults.border(
        border = Border.None,
        focusedBorder = Border.None,
        pressedBorder = Border.None
    )
}

/**
 * An action Card's fill: frosted at rest, solid white with black content on focus. The label
 * must read LocalContentColor (or [actionCardContentColor]) to follow it. A disabled action
 * ([enabled] false) stays frosted on focus.
 */
@Composable
internal fun expressiveActionCardColors(upstream: CardColors, enabled: Boolean = true): CardColors {
    if (!isExpressiveBuild) return upstream
    val focusedContainer = if (enabled) ExpressiveColors.focus else ExpressiveColors.frostedFocused
    val focusedContent = if (enabled) ExpressiveColors.onFocus else ExpressiveColors.textTertiary
    return CardDefaults.colors(
        containerColor = ExpressiveColors.frosted,
        contentColor = if (enabled) ExpressiveColors.textPrimary else ExpressiveColors.textTertiary,
        focusedContainerColor = focusedContainer,
        focusedContentColor = focusedContent,
        pressedContainerColor = focusedContainer,
        pressedContentColor = focusedContent
    )
}

/** The label colour inside an action Card: the card's content colour on the box build. */
@Composable
@ReadOnlyComposable
internal fun actionCardContentColor(upstream: Color): Color =
    if (isExpressiveBuild) LocalContentColor.current else upstream

/** Grows a focused action or option Card as much as the other expressive controls. */
internal fun expressiveCardScale(upstream: CardScale): CardScale =
    if (!isExpressiveBuild) {
        upstream
    } else {
        CardDefaults.scale(focusedScale = ExpressiveFocus.CONTROL_SCALE)
    }

/** The corner of a dialog drawn by hand instead of through NuvioDialog. */
internal fun expressiveDialogShape(upstream: Shape): Shape =
    if (isExpressiveBuild) ExpressiveContainerShape else upstream

/** A label inside a tv-material Button or Card: its text style, at least 16sp on the box. */
@Composable
@ReadOnlyComposable
internal fun buttonLabelStyle(): TextStyle = LocalTextStyle.current.bodyFloor()
