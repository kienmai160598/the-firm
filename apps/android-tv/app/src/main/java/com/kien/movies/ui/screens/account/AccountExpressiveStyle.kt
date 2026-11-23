package com.kien.movies.ui.screens.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kien.movies.ui.theme.MoviesTheme

// The account screens take the theme's roles for their own chrome, so sign-in, QR and server
// dialogs share the launcher's palette.

private const val ERROR_CONTAINER_ALPHA = 0.14f
private const val FOCUSED_ERROR_CONTAINER_ALPHA = 0.26f
private const val GRADIENT_ACCENT_ALPHA = 0.32f
private const val GRADIENT_ACCENT_STOP = 0.14f
private const val GRADIENT_FADE_STOP = 0.7f

@Composable
@ReadOnlyComposable
internal fun accountTextPrimary(): Color = MoviesTheme.colors.TextPrimary

@Composable
@ReadOnlyComposable
internal fun accountTextSecondary(): Color = MoviesTheme.colors.TextSecondary

/** The login pane beside the brand panel: M3's low surface container. */
@Composable
@ReadOnlyComposable
internal fun accountPaneColor(): Color = MoviesTheme.colors.Panel

/** Dividers and resting outlines: M3's outline variant. */
@Composable
@ReadOnlyComposable
internal fun accountOutlineColor(): Color = MoviesTheme.colors.Border

/** A resting control or placeholder block: M3's high surface container. */
@Composable
@ReadOnlyComposable
internal fun accountControlColor(): Color = MoviesTheme.colors.BackgroundCard

/** The screen's base under the gradient. */
@Composable
@ReadOnlyComposable
internal fun accountScreenBase(): Color = MoviesTheme.colors.Background

/** The gradient's lit corner: the accent's container, faded into the base. */
@Composable
@ReadOnlyComposable
internal fun accountGradientAccent(): Color = MoviesTheme.colors.SecondaryVariant.copy(alpha = GRADIENT_ACCENT_ALPHA)

@Composable
@ReadOnlyComposable
internal fun accountSuccessColor(): Color = MoviesTheme.colors.Success

@Composable
@ReadOnlyComposable
internal fun accountErrorContent(): Color = MoviesTheme.colors.Error

@Composable
@ReadOnlyComposable
internal fun accountErrorContainer(): Color = MoviesTheme.colors.Error.copy(alpha = ERROR_CONTAINER_ALPHA)

@Composable
@ReadOnlyComposable
internal fun accountFocusedErrorContainer(): Color = MoviesTheme.colors.Error.copy(alpha = FOCUSED_ERROR_CONTAINER_ALPHA)

@Composable
@ReadOnlyComposable
internal fun accountWarningContainer(): Color = MoviesTheme.colors.Warning.copy(alpha = ERROR_CONTAINER_ALPHA)

@Composable
@ReadOnlyComposable
internal fun accountWarningContent(): Color = MoviesTheme.colors.Warning

/** The sign-in backdrop: [accent] in the top corner, fading into [base] along the diagonal. */
internal fun Modifier.accountGradientBackground(accent: Color, base: Color): Modifier = drawWithCache {
    val brush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to accent,
            GRADIENT_ACCENT_STOP to accent,
            GRADIENT_FADE_STOP to base,
            1f to base
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )
    onDrawBehind { drawRect(brush = brush) }
}

/** One line on the box, where larger text could crowd a column. */
internal val tvSingleLineMax: Int = 1
