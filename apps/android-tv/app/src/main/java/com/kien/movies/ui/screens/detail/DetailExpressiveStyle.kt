package com.kien.movies.ui.screens.detail

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import com.kien.movies.ui.theme.MoviesTheme

private const val ERROR_MESSAGE_TINT = 0.24f

/** The detail page's toast: a raised M3 surface, tinted with the theme's error tone if failed. */
@Composable
internal fun detailMessageContainer(isError: Boolean): Color {
    val raised = MoviesTheme.colors.BackgroundElevated
    if (!isError) return raised

    return MoviesTheme.colors.Error.copy(alpha = ERROR_MESSAGE_TINT)
        .compositeOver(MoviesTheme.colors.BackgroundCard)
}

internal fun detailMessageShape(): Shape = RoundedCornerShape(MoviesTheme.radii.lg)

/** The hero's outlined age and status tags: pills, like M3 assist chips. */
internal fun heroBadgeShape(): Shape = CircleShape

/** The star row fills with the focus tone instead of tinting and ringing. */
@Composable
internal fun ratingStarsContainer(focused: Boolean): Color = if (focused) MoviesTheme.colors.FocusBackground else Color.Transparent

/** The episode overlay sits on the theme's background, not a near-black. */
@Composable
internal fun episodeOverlaySurface(): Color = MoviesTheme.colors.Background

@Composable
internal fun episodeOverlayNoneGradient(): List<Color> {
    val colors = MoviesTheme.colors
    return listOf(colors.Background, colors.Background, colors.BackgroundElevated)
}

@Composable
internal fun shufflePreviewGradient(): List<Color> {
    val colors = MoviesTheme.colors
    return listOf(colors.Background, colors.BackgroundElevated, colors.BackgroundCard)
}
