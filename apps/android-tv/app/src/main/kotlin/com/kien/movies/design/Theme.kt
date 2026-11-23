package com.kien.movies.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

/**
 * tv-material's theme filled from [Palette] and [TextStyles], so the stock TV components (Button,
 * ListItem, NavigationDrawerItem, Card) take the tvOS look without per call overrides:
 * a focused control is solid white with black content ([ColorScheme.onSurface] and
 * [ColorScheme.inverseOnSurface] for buttons, [ColorScheme.inverseSurface] for drawer items).
 *
 * No Surface at the root: the screen colour is the window background (shell/WindowBackground),
 * so a full screen fill is drawn once per frame, not twice. Text defaults to white.
 */
@Composable
fun MoviesTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = moviesColorScheme, typography = moviesTypography) {
        CompositionLocalProvider(LocalContentColor provides Palette.textPrimary, content = content)
    }
}

private val moviesColorScheme: ColorScheme = darkColorScheme(
    primary = Palette.focusedContainer,
    onPrimary = Palette.focusedContent,
    secondary = Palette.textSecondary,
    secondaryContainer = Palette.frostRaised,
    onSecondaryContainer = Palette.textPrimary,
    background = Palette.homeBackground,
    onBackground = Palette.textPrimary,
    surface = Palette.homeBackground,
    onSurface = Palette.focusedContainer,
    surfaceVariant = Palette.frostRest,
    onSurfaceVariant = Palette.textSecondary,
    inverseSurface = Palette.focusedContainer,
    inverseOnSurface = Palette.focusedContent,
    border = Palette.frostStrong,
    borderVariant = Palette.frostRest,
    scrim = Color.Black
)

// Roles mapped to the tvOS sizes; screens may use either the role or the named TextStyles token.
private val moviesTypography = Typography(
    displayMedium = TextStyles.detailsTitle,
    displaySmall = TextStyles.heroTitle,
    headlineMedium = TextStyles.playerTitle,
    headlineSmall = TextStyles.streamsTitle,
    titleMedium = TextStyles.shelfHeader,
    titleSmall = TextStyles.cardTitle,
    bodyLarge = TextStyles.body,
    bodyMedium = TextStyles.body,
    bodySmall = TextStyles.small,
    labelLarge = TextStyles.button,
    labelMedium = TextStyles.small,
    labelSmall = TextStyles.badge
)
