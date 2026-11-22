@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.settings

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import com.kien.movies.ui.components.expressiveButtonColors
import com.kien.movies.ui.components.expressivePrimaryButtonColors
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.ExpressiveRadii
import com.kien.movies.ui.theme.NuvioTheme
import com.kien.movies.ui.theme.expressiveFocusScale
import com.kien.movies.ui.theme.isExpressiveBuild

// Settings rows span the panel, so they grow less than a chip or a tab does.
private const val SETTINGS_ROW_FOCUS_SCALE = 1.02f
private const val EXPRESSIVE_ROW_FOCUS_SCALE = 1.03f
private const val OPTION_FOCUS_SCALE = 1.02f
private const val OPTION_SUPPORTING_ALPHA = 0.72f

internal const val SETTINGS_SECONDARY_TEXT_ALPHA = 0.64f
internal const val SETTINGS_TERTIARY_TEXT_ALPHA = 0.40f

/** A resting settings row on the box build: white at 8%, so the list reads as tvOS tiles. */
internal val SettingsRowRestFill: Color = Color.White.copy(alpha = 0.08f)

private val ExpressiveRowShape = RoundedCornerShape(ExpressiveRadii.forBuild.md)
private val CapsuleShape = RoundedCornerShape(percent = ExpressiveFocus.PILL_PERCENT)

internal val SettingsStillButtonScale: ButtonScale =
    ButtonDefaults.scale(focusedScale = 1f, pressedScale = 1f)

@Composable
internal fun Modifier.settingsRowFocusScale(focused: Boolean): Modifier = expressiveFocusScale(
    focused,
    if (isExpressiveBuild) EXPRESSIVE_ROW_FOCUS_SCALE else SETTINGS_ROW_FOCUS_SCALE
)

@Composable
@ReadOnlyComposable
internal fun settingsRowCornerShape(): RoundedCornerShape =
    if (isExpressiveBuild) ExpressiveRowShape else RoundedCornerShape(NuvioTheme.radii.xl)

/**
 * A settings row on the box build, in the tvOS Settings manner: frosted at rest with white
 * content, solid white with black content on focus. Text and icons inside read the content colour
 * through [settingsRowText], so they turn black with the fill.
 */
@Composable
internal fun expressiveSettingsRowColors(): CardColors = CardDefaults.colors(
    containerColor = SettingsRowRestFill,
    contentColor = ExpressiveColors.textPrimary,
    focusedContainerColor = ExpressiveColors.focus,
    focusedContentColor = ExpressiveColors.onFocus,
    pressedContainerColor = ExpressiveColors.secondaryPressed,
    pressedContentColor = ExpressiveColors.onFocus
)

/**
 * The colour of text or an icon in a settings row: the row's content colour at [emphasis] on the
 * box build, so it follows the white focus fill; [upstream] elsewhere. [contentAlpha] dims a
 * disabled row on both.
 */
@Composable
@ReadOnlyComposable
internal fun settingsRowText(
    upstream: Color,
    contentAlpha: Float = 1f,
    emphasis: Float = 1f
): Color = if (isExpressiveBuild) {
    LocalContentColor.current.copy(alpha = emphasis * contentAlpha)
} else {
    upstream.copy(alpha = contentAlpha)
}

internal fun settingsOptionScale(): Float = if (isExpressiveBuild) OPTION_FOCUS_SCALE else 1f

@Composable
@ReadOnlyComposable
internal fun settingsOptionShape(): RoundedCornerShape = RoundedCornerShape(NuvioTheme.radii.lg)

/** A picker option: tonal when chosen, accent when focused; its text follows LocalContentColor. */
@Composable
internal fun settingsOptionColors(selected: Boolean): CardColors = if (isExpressiveBuild) {
    CardDefaults.colors(
        containerColor = if (selected) ExpressiveColors.frostedStrong else SettingsRowRestFill,
        contentColor = ExpressiveColors.textPrimary,
        focusedContainerColor = ExpressiveColors.focus,
        focusedContentColor = ExpressiveColors.onFocus,
        pressedContainerColor = ExpressiveColors.secondaryPressed,
        pressedContentColor = ExpressiveColors.onFocus
    )
} else {
    upstreamOptionColors(selected)
}

@Composable
private fun upstreamOptionColors(selected: Boolean): CardColors = CardDefaults.colors(
    containerColor =
        if (selected) NuvioTheme.colors.FocusBackground else NuvioTheme.colors.BackgroundCard,
    contentColor = NuvioTheme.colors.TextPrimary,
    focusedContainerColor = NuvioTheme.colors.Secondary,
    focusedContentColor = NuvioTheme.colors.OnSecondary
)

@Composable
@ReadOnlyComposable
internal fun settingsOptionSupportingColor(): Color =
    LocalContentColor.current.copy(alpha = OPTION_SUPPORTING_ALPHA)

/**
 * The primary action rests on the accent's container so focus, which fills with the accent
 * itself, still reads; the other actions rest tonal.
 */
@Composable
internal fun settingsDialogButtonColors(primary: Boolean, focused: Boolean): ButtonColors {
    if (isExpressiveBuild) {
        val upstream = ButtonDefaults.colors()
        return if (primary) {
            expressivePrimaryButtonColors(upstream)
        } else {
            expressiveButtonColors(upstream)
        }
    }
    val container = ExpressiveFocus.containerColor(
        focused = focused,
        resting =
            if (primary) NuvioTheme.colors.SecondaryVariant else NuvioTheme.colors.FocusBackground
    )
    val content = ExpressiveFocus.contentColor(
        focused = focused,
        resting =
            if (primary) NuvioTheme.colors.OnSecondaryVariant else NuvioTheme.colors.TextPrimary
    )
    return ButtonDefaults.colors(
        containerColor = container,
        contentColor = content,
        focusedContainerColor = container,
        focusedContentColor = content,
        pressedContainerColor = container,
        pressedContentColor = content
    )
}

@Composable
internal fun settingsDialogButtonShape(focused: Boolean): ButtonShape =
    if (isExpressiveBuild) {
        ButtonDefaults.shape(CapsuleShape)
    } else {
        ButtonDefaults.shape(ExpressiveFocus.morphingPill(focused))
    }

/** A capsule in every state on the box build, as tvOS draws its buttons and chips. */
internal fun settingsCapsuleOr(upstream: Shape): Shape =
    if (isExpressiveBuild) CapsuleShape else upstream

/**
 * A row's trailing value: white at 64% at rest and black at 64% on focus on the box build. A value
 * given its own colour keeps it at rest, since that colour carries meaning (an error, a state).
 */
@Composable
@ReadOnlyComposable
internal fun settingsRowValueColor(valueColor: Color, contentAlpha: Float, focused: Boolean): Color =
    if (isExpressiveBuild && (focused || valueColor == NuvioTheme.colors.TextSecondary)) {
        settingsRowText(valueColor, contentAlpha, SETTINGS_SECONDARY_TEXT_ALPHA)
    } else {
        valueColor.copy(alpha = contentAlpha)
    }
