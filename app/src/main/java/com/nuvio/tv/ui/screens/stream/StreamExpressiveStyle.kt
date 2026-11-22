@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.stream

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.nuvio.tv.ui.theme.ExpressiveColors
import com.nuvio.tv.ui.theme.ExpressiveFocus
import com.nuvio.tv.ui.theme.ExpressiveMotion
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.theme.isExpressiveBuild

private const val STREAM_CARD_FOCUS_SCALE = 1.04f
private const val STREAM_PILL_FOCUS_SCALE = 1.04f
private const val LIFT_DAMPING = 0.8f
private const val LIFT_STIFFNESS = 500f
private const val ROW_SUPPORTING_ALPHA = 0.64f
private const val FOCUSED_SUPPORTING_ALPHA = 0.7f
private const val PANEL_SCRIM_ALPHA = 0.45f
private const val UPSTREAM_CARD_FOCUS_SCALE = 1.02f

private val StreamPanelRadius = 20.dp
private val CapsuleShape = RoundedCornerShape(percent = 50)

/** Apple TV lift: a focused part grows on a smooth spring, read in the draw layer only. */
@Composable
private fun Modifier.streamLift(focused: Boolean, scale: Float): Modifier {
    val current = animateFloatAsState(
        targetValue = if (focused) scale else 1f,
        animationSpec = spring(dampingRatio = LIFT_DAMPING, stiffness = LIFT_STIFFNESS),
        label = "streamLift"
    )
    return graphicsLayer {
        scaleX = current.value
        scaleY = current.value
    }
}

@Composable
internal fun Modifier.streamCardFocusScale(focused: Boolean): Modifier =
    if (isExpressiveBuild) {
        streamLift(focused, STREAM_CARD_FOCUS_SCALE)
    } else {
        streamLift(focused, UPSTREAM_CARD_FOCUS_SCALE)
    }

@Composable
internal fun Modifier.streamPillFocusScale(focused: Boolean): Modifier =
    streamLift(focused, STREAM_PILL_FOCUS_SCALE)

/** The same [outline] in every state, so tv-material's default focus ring never shows. */
@Composable
internal fun streamCardBorder(outline: Border): CardBorder =
    CardDefaults.border(border = outline, focusedBorder = outline, pressedBorder = outline)

/** A frosted row at rest, solid white on focus. */
@Composable
internal fun streamCardColors(): CardColors = if (isExpressiveBuild) {
    CardDefaults.colors(
        containerColor = ExpressiveColors.frosted,
        contentColor = ExpressiveColors.textPrimary,
        focusedContainerColor = ExpressiveColors.focus,
        focusedContentColor = ExpressiveColors.onFocus,
        pressedContainerColor = ExpressiveColors.focus,
        pressedContentColor = ExpressiveColors.onFocus
    )
} else {
    CardDefaults.colors(
        containerColor = NuvioTheme.colors.BackgroundElevated,
        focusedContainerColor = NuvioTheme.colors.FocusBackground
    )
}

/** Whether a ring may mark focus; the Apple TV look marks focus with the white fill alone. */
@Composable
internal fun streamCardUsesFocusRing(): Boolean =
    !isExpressiveBuild && NuvioTheme.palette.focusRingGradient.size > 1

/** Primary text of a stream row: black on the white focused row, white otherwise. */
@Composable
internal fun streamRowTextColor(cardFocused: Boolean): Color = when {
    !isExpressiveBuild -> NuvioTheme.colors.TextPrimary
    cardFocused -> ExpressiveColors.onFocus
    else -> ExpressiveColors.textPrimary
}

/** Secondary text of a stream row, such as its description. */
@Composable
internal fun streamRowSupportingColor(cardFocused: Boolean): Color = when {
    !isExpressiveBuild -> NuvioTheme.extendedColors.textSecondary
    cardFocused -> ExpressiveColors.onFocus.copy(alpha = FOCUSED_SUPPORTING_ALPHA)
    else -> Color.White.copy(alpha = ROW_SUPPORTING_ALPHA)
}

// A plain pick, not an animated colour, so a focus change does not recompose the card per frame.
@Composable
internal fun streamAddonNameColor(cardFocused: Boolean): Color = when {
    isExpressiveBuild -> streamRowSupportingColor(cardFocused)
    cardFocused -> NuvioTheme.extendedColors.textSecondary
    else -> NuvioTheme.extendedColors.textTertiary
}

@Composable
internal fun streamPillColors(focused: Boolean, resting: Color): CardColors {
    val container = if (isExpressiveBuild) {
        animateColorAsState(
            targetValue = if (focused) ExpressiveColors.focus else ExpressiveColors.frosted,
            animationSpec = ExpressiveMotion.defaultEffects(),
            label = "streamPillContainer"
        ).value
    } else {
        ExpressiveFocus.containerColor(focused, resting = resting)
    }
    return CardDefaults.colors(
        containerColor = container,
        focusedContainerColor = container,
        pressedContainerColor = container
    )
}

@Composable
internal fun streamPillContentColor(focused: Boolean): Color = if (isExpressiveBuild) {
    if (focused) ExpressiveColors.onFocus else ExpressiveColors.textPrimary
} else {
    ExpressiveFocus.contentColor(focused, resting = NuvioTheme.colors.TextPrimary)
}

@Composable
internal fun streamPillShape(focused: Boolean): CardShape = if (isExpressiveBuild) {
    CardDefaults.shape(shape = CapsuleShape)
} else {
    CardDefaults.shape(shape = ExpressiveFocus.morphingPill(focused))
}

/** The panel that holds the stream list: a translucent dark scrim, never a blur. */
internal object StreamPanelStyle {
    val usesBlur: Boolean = !isExpressiveBuild

    @Composable
    fun radius(): Dp = if (isExpressiveBuild) StreamPanelRadius else NuvioTheme.radii.panel

    @Composable
    fun background(blurred: Boolean): Color = when {
        isExpressiveBuild -> Color.Black.copy(alpha = PANEL_SCRIM_ALPHA)
        blurred -> NuvioTheme.colors.Panel.copy(alpha = 0.65f)
        else -> NuvioTheme.colors.BackgroundCard.copy(alpha = 0.5f)
    }
}

/** The title column: centred upstream, left aligned like Apple TV on the expressive build. */
internal object StreamTitleColumnStyle {
    val horizontalAlignment: Alignment.Horizontal =
        if (isExpressiveBuild) Alignment.Start else Alignment.CenterHorizontally
    val logoAlignment: Alignment =
        if (isExpressiveBuild) Alignment.CenterStart else Alignment.Center
    val textAlign: TextAlign = if (isExpressiveBuild) TextAlign.Start else TextAlign.Center
}

/** Horizontal stops of the backdrop scrim, dark behind the title column and over the list. */
internal fun streamBackdropScrimStops(): List<Pair<Float, Float>> = if (isExpressiveBuild) {
    listOf(0.0f to 0.92f, 0.30f to 0.70f, 0.50f to 0.45f, 0.75f to 0.55f, 1.0f to 0.75f)
} else {
    listOf(
        0.0f to 1f, 0.15f to 0.85f, 0.30f to 0.40f, 0.50f to 0.15f,
        0.70f to 0.40f, 0.85f to 0.85f, 1.0f to 1f
    )
}
