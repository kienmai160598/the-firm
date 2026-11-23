@file:OptIn(ExperimentalTvMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.kien.movies.ui.screens.settings

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import com.kien.movies.ui.theme.ExpressiveRadii
import com.kien.movies.ui.theme.MoviesTheme

private const val OPTION_SUPPORTING_ALPHA = 0.72f

internal const val SETTINGS_SECONDARY_TEXT_ALPHA = 0.64f
internal const val SETTINGS_TERTIARY_TEXT_ALPHA = 0.40f

private val RowCornerShape = RoundedCornerShape(ExpressiveRadii.tokens.md)

@Composable
@ReadOnlyComposable
internal fun settingsRowCornerShape(): RoundedCornerShape = RowCornerShape

/**
 * The colour of text or an icon in a settings row: the row's content colour at [emphasis], so it
 * follows the white focus fill. [contentAlpha] dims a disabled row.
 */
@Composable
@ReadOnlyComposable
internal fun settingsRowText(contentAlpha: Float = 1f, emphasis: Float = 1f): Color = LocalContentColor.current.copy(alpha = emphasis * contentAlpha)

@Composable
@ReadOnlyComposable
internal fun settingsOptionSupportingColor(): Color = LocalContentColor.current.copy(alpha = OPTION_SUPPORTING_ALPHA)

/**
 * A row's trailing value: white at 64% at rest and black at 64% on focus. A value given its own
 * colour keeps it at rest, since that colour carries meaning (an error, a state).
 */
@Composable
@ReadOnlyComposable
internal fun settingsRowValueColor(
    valueColor: Color,
    contentAlpha: Float,
    focused: Boolean
): Color = if (focused || valueColor == MoviesTheme.colors.TextSecondary) {
    settingsRowText(contentAlpha, SETTINGS_SECONDARY_TEXT_ALPHA)
} else {
    valueColor.copy(alpha = contentAlpha)
}
