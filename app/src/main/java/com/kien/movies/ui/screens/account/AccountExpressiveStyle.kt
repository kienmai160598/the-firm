@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.account

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kien.movies.ui.components.destructiveButtonColors
import com.kien.movies.ui.theme.ExpressiveRadii
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.isExpressiveBuild

// The account screens draw their own chrome in fixed colours upstream. On the expressive build
// they take the theme's roles instead, so sign-in, QR and server dialogs share the launcher's
// palette; other builds keep upstream's values, which the call sites pass in.

private const val ERROR_CONTAINER_ALPHA = 0.14f
private const val FOCUSED_ERROR_CONTAINER_ALPHA = 0.26f
private const val GRADIENT_ACCENT_ALPHA = 0.32f
private const val GRADIENT_ACCENT_STOP = 0.14f
private const val GRADIENT_FADE_STOP = 0.7f

private val ExpressiveRowShape = RoundedCornerShape(ExpressiveRadii.forBuild.md)

/** [expressive] on the expressive build, [upstream] elsewhere. */
@Composable
@ReadOnlyComposable
private fun themed(expressive: Color, upstream: Color): Color =
    if (isExpressiveBuild) expressive else upstream

@Composable
@ReadOnlyComposable
internal fun accountTextPrimary(upstream: Color): Color =
    themed(MoviesTheme.colors.TextPrimary, upstream)

@Composable
@ReadOnlyComposable
internal fun accountTextSecondary(upstream: Color): Color =
    themed(MoviesTheme.colors.TextSecondary, upstream)

/** The login pane beside the brand panel: M3's low surface container. */
@Composable
@ReadOnlyComposable
internal fun accountPaneColor(upstream: Color): Color = themed(MoviesTheme.colors.Panel, upstream)

/** Dividers and resting outlines: M3's outline variant. */
@Composable
@ReadOnlyComposable
internal fun accountOutlineColor(upstream: Color): Color =
    themed(MoviesTheme.colors.Border, upstream)

/** A resting control or placeholder block: M3's high surface container. */
@Composable
@ReadOnlyComposable
internal fun accountControlColor(upstream: Color): Color =
    themed(MoviesTheme.colors.BackgroundCard, upstream)

/** The screen's base under the gradient. */
@Composable
@ReadOnlyComposable
internal fun accountScreenBase(upstream: Color): Color =
    themed(MoviesTheme.colors.Background, upstream)

/** The gradient's lit corner: the accent's container, faded into the base. */
@Composable
@ReadOnlyComposable
internal fun accountGradientAccent(upstream: Color): Color = themed(
    MoviesTheme.colors.SecondaryVariant.copy(alpha = GRADIENT_ACCENT_ALPHA),
    upstream
)

@Composable
@ReadOnlyComposable
internal fun accountSuccessColor(upstream: Color): Color =
    themed(MoviesTheme.colors.Success, upstream)

@Composable
@ReadOnlyComposable
internal fun accountErrorContent(upstream: Color): Color = themed(MoviesTheme.colors.Error, upstream)

@Composable
@ReadOnlyComposable
internal fun accountErrorContainer(upstream: Color): Color = themed(
    MoviesTheme.colors.Error.copy(alpha = ERROR_CONTAINER_ALPHA),
    upstream
)

@Composable
@ReadOnlyComposable
internal fun accountFocusedErrorContainer(upstream: Color): Color = themed(
    MoviesTheme.colors.Error.copy(alpha = FOCUSED_ERROR_CONTAINER_ALPHA),
    upstream
)

/** A row nested in a card: M3's medium corner, one step under the card's. */
internal fun expressiveRowShape(upstream: Shape): Shape =
    if (isExpressiveBuild) ExpressiveRowShape else upstream

@Composable
@ReadOnlyComposable
internal fun accountWarningContainer(upstream: Color): Color = themed(
    MoviesTheme.colors.Warning.copy(alpha = ERROR_CONTAINER_ALPHA),
    upstream
)

/** No outline around a tonal message on the expressive build. */
@Composable
@ReadOnlyComposable
internal fun accountMessageOutline(upstream: Color): Color = themed(Color.Transparent, upstream)

@Composable
@ReadOnlyComposable
internal fun accountWarningContent(upstream: Color): Color =
    themed(MoviesTheme.colors.Warning, upstream)

/** Sign out and other destructive actions: M3's error container, the error colour on focus. */
@Composable
internal fun accountDestructiveColors(upstream: ButtonColors): ButtonColors =
    if (isExpressiveBuild) destructiveButtonColors() else upstream

/** The sign-in backdrop: [accent] in the top corner, fading into [base] along the diagonal. */
internal fun Modifier.accountGradientBackground(accent: Color, base: Color): Modifier =
    drawWithCache {
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

/** One line on the box, where larger text could crowd a column; upstream wraps freely. */
internal val tvSingleLineMax: Int = if (isExpressiveBuild) 1 else Int.MAX_VALUE
