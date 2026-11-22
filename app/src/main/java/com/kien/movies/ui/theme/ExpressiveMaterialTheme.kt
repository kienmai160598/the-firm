package com.kien.movies.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ColorScheme as TvColorScheme
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Typography as TvTypography

/**
 * Gives the androidx material3 parts (player side panels, menus, text fields) the app's palette
 * and font; without it they render Roboto in M3's baseline purple.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ExpressiveMaterialTheme(content: @Composable () -> Unit) {
    val colors = MoviesTheme.colors
    val palette = MoviesTheme.palette
    val tvTypography = TvMaterialTheme.typography
    val colorScheme = remember(colors, palette) { materialColorScheme(colors, palette) }
    val typography = remember(tvTypography) { tvTypography.toMaterialTypography() }
    val appIndication = LocalIndication.current

    MaterialTheme(colorScheme = colorScheme, typography = typography) {
        // material3 provides its ripple here; plain clickables keep the indication they had.
        CompositionLocalProvider(LocalIndication provides appIndication, content = content)
    }
}

/** The tv scheme's container and border roles from the palette instead of tv's baseline. */
@OptIn(ExperimentalTvMaterial3Api::class)
internal fun TvColorScheme.withPaletteRoles(
    colors: MoviesColorScheme,
    palette: ThemeColorPalette
): TvColorScheme = copy(
    primaryContainer = colors.SecondaryVariant,
    onPrimaryContainer = colors.OnSecondaryVariant,
    secondaryContainer = colors.FocusBackground,
    onSecondaryContainer = colors.FocusContent,
    border = palette.outline,
    borderVariant = colors.Border
)

private fun materialColorScheme(
    colors: MoviesColorScheme,
    palette: ThemeColorPalette
): ColorScheme = darkColorScheme(
    primary = colors.Secondary,
    onPrimary = colors.OnSecondary,
    primaryContainer = colors.SecondaryVariant,
    onPrimaryContainer = colors.OnSecondaryVariant,
    secondary = colors.Secondary,
    onSecondary = colors.OnSecondary,
    secondaryContainer = colors.FocusBackground,
    onSecondaryContainer = colors.FocusContent,
    background = colors.Background,
    onBackground = colors.TextPrimary,
    surface = colors.Background,
    onSurface = colors.TextPrimary,
    surfaceVariant = colors.SurfaceVariant,
    onSurfaceVariant = colors.TextSecondary,
    surfaceTint = colors.Secondary,
    surfaceDim = colors.Background,
    surfaceContainerLowest = colors.Background,
    surfaceContainerLow = colors.Panel,
    surfaceContainer = colors.Surface,
    surfaceContainerHigh = colors.BackgroundCard,
    surfaceContainerHighest = colors.SurfaceVariant,
    error = colors.Error,
    outline = palette.outline,
    outlineVariant = colors.Border
)

@OptIn(ExperimentalTvMaterial3Api::class)
private fun TvTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = displayLarge,
    displayMedium = displayMedium,
    displaySmall = displaySmall,
    headlineLarge = headlineLarge,
    headlineMedium = headlineMedium,
    headlineSmall = headlineSmall,
    titleLarge = titleLarge,
    titleMedium = titleMedium,
    titleSmall = titleSmall,
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = bodySmall,
    labelLarge = labelLarge,
    labelMedium = labelMedium,
    labelSmall = labelSmall
)
