@file:OptIn(ExperimentalTvMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

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
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.components.expressive.moviesButtonColors
import com.kien.movies.ui.components.expressive.moviesButtonShape
import com.kien.movies.ui.components.expressive.moviesCardColors
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.ExpressiveRadii
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.expressiveFocusScale

private const val OPTION_SUPPORTING_ALPHA = 0.72f

internal const val SETTINGS_SECONDARY_TEXT_ALPHA = 0.64f
internal const val SETTINGS_TERTIARY_TEXT_ALPHA = 0.40f

/** A resting settings row: the kit's frosted fill, so the list reads as tvOS tiles. */
internal val SettingsRowRestFill: Color = ExpressiveColors.frosted

private val RowCornerShape = RoundedCornerShape(ExpressiveRadii.tokens.md)

internal val SettingsStillButtonScale: ButtonScale =
    ButtonDefaults.scale(focusedScale = 1f, pressedScale = 1f)

@Composable
internal fun Modifier.settingsRowFocusScale(focused: Boolean): Modifier =
    expressiveFocusScale(focused, ExpressiveFocus.CONTROL_SCALE)

@Composable
@ReadOnlyComposable
internal fun settingsRowCornerShape(): RoundedCornerShape = RowCornerShape

/**
 * The colour of text or an icon in a settings row: the row's content colour at [emphasis], so it
 * follows the white focus fill. [contentAlpha] dims a disabled row.
 */
@Composable
@ReadOnlyComposable
internal fun settingsRowText(
    contentAlpha: Float = 1f,
    emphasis: Float = 1f
): Color = LocalContentColor.current.copy(alpha = emphasis * contentAlpha)

internal fun settingsOptionScale(): Float = ExpressiveFocus.CONTROL_SCALE

@Composable
@ReadOnlyComposable
internal fun settingsOptionShape(): RoundedCornerShape = RowCornerShape

/** A picker option: brighter frosted when chosen, white on focus; its text follows LocalContentColor. */
@Composable
internal fun settingsOptionColors(selected: Boolean): CardColors = moviesCardColors(selected = selected)

@Composable
@ReadOnlyComposable
internal fun settingsOptionSupportingColor(): Color =
    LocalContentColor.current.copy(alpha = OPTION_SUPPORTING_ALPHA)

/** The kit's button fills: the brighter frosted capsule for the [primary] action. */
@Composable
internal fun settingsDialogButtonColors(primary: Boolean, focused: Boolean): ButtonColors =
    moviesButtonColors(if (primary) MoviesButtonStyle.Primary else MoviesButtonStyle.Secondary)

@Composable
internal fun settingsDialogButtonShape(focused: Boolean): ButtonShape = moviesButtonShape

/**
 * A row's trailing value: white at 64% at rest and black at 64% on focus. A value given its own
 * colour keeps it at rest, since that colour carries meaning (an error, a state).
 */
@Composable
@ReadOnlyComposable
internal fun settingsRowValueColor(valueColor: Color, contentAlpha: Float, focused: Boolean): Color =
    if (focused || valueColor == MoviesTheme.colors.TextSecondary) {
        settingsRowText(contentAlpha, SETTINGS_SECONDARY_TEXT_ALPHA)
    } else {
        valueColor.copy(alpha = contentAlpha)
    }
