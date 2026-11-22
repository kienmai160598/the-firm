package com.kien.movies.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.kien.movies.domain.model.AppFont
import com.kien.movies.domain.model.AppTheme
import com.kien.movies.domain.model.CustomThemeColors
import com.kien.movies.domain.model.SettingsUiStyle

data class MoviesExtendedColors(
    val backgroundElevated: Color,
    val backgroundCard: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val focusRing: Color,
    val focusBackground: Color,
    val rating: Color
)

val LocalMoviesColors = staticCompositionLocalOf {
    MoviesColorScheme(ThemeColors.Ocean)
}

val LocalMoviesExtendedColors = staticCompositionLocalOf {
    MoviesExtendedColors(
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF242424),
        textSecondary = Color(0xFFB3B3B3),
        textTertiary = Color(0xFF808080),
        focusRing = ThemeColors.Ocean.focusRing,
        focusBackground = ThemeColors.Ocean.focusBackground,
        rating = Color(0xFFFFD700)
    )
}

val LocalMoviesTextStyles = staticCompositionLocalOf { MoviesTextStyles }

val LocalAppTheme = staticCompositionLocalOf { AppTheme.WHITE }

val LocalThemePalette = staticCompositionLocalOf { ThemeColors.White }

val LocalSettingsUiStyle = staticCompositionLocalOf { SettingsUiStyle.CLASSIC }

val LocalMoviesFocusRingStyle = staticCompositionLocalOf {
    createFocusRingStyle(ThemeColors.Ocean)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MoviesTheme(
    appTheme: AppTheme = AppTheme.WHITE,
    appFont: AppFont = AppFont.INTER,
    amoledMode: Boolean = false,
    amoledSurfacesMode: Boolean = false,
    settingsUiStyle: SettingsUiStyle = SettingsUiStyle.CLASSIC,
    customThemeColors: CustomThemeColors = CustomThemeColors.Default,
    content: @Composable () -> Unit
) {
    val palette = androidx.compose.runtime.remember(appTheme, customThemeColors) {
        ThemeColors.getColorPalette(appTheme, customThemeColors)
    }
    val focusRingStyle = createFocusRingStyle(palette)
    val colorScheme = MoviesColorScheme(
        palette = palette,
        amoledMode = amoledMode,
        amoledSurfacesMode = amoledSurfacesMode
    )
    val typography = buildAppTypography(appFont)
    val textStyles = buildMoviesTextStyles(typography)

    val materialColorScheme = darkColorScheme(
        primary = colorScheme.Primary,
        onPrimary = colorScheme.OnPrimary,
        secondary = colorScheme.Secondary,
        onSecondary = colorScheme.OnSecondary,
        background = colorScheme.Background,
        surface = colorScheme.Surface,
        surfaceVariant = colorScheme.SurfaceVariant,
        onBackground = colorScheme.TextPrimary,
        onSurface = colorScheme.TextPrimary,
        onSurfaceVariant = colorScheme.TextSecondary,
        error = colorScheme.Error
    )

    val extendedColors = MoviesExtendedColors(
        backgroundElevated = colorScheme.BackgroundElevated,
        backgroundCard = colorScheme.BackgroundCard,
        textSecondary = colorScheme.TextSecondary,
        textTertiary = colorScheme.TextTertiary,
        focusRing = colorScheme.FocusRing,
        focusBackground = colorScheme.FocusBackground,
        rating = colorScheme.Rating
    )

    CompositionLocalProvider(
        LocalMoviesColors provides colorScheme,
        LocalMoviesExtendedColors provides extendedColors,
        LocalMoviesTextStyles provides textStyles,
        LocalAppTheme provides appTheme,
        LocalThemePalette provides palette,
        LocalSettingsUiStyle provides settingsUiStyle,
        LocalMoviesFocusRingStyle provides focusRingStyle
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme.withPaletteRoles(colorScheme, palette),
            typography = typography,
            content = { ExpressiveMaterialTheme(content) }
        )
    }
}

object MoviesTheme {
    val palette: ThemeColorPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalThemePalette.current

    val colors: MoviesColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalMoviesColors.current

    val extendedColors: MoviesExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMoviesExtendedColors.current

    val textStyles: MoviesTextStyleTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalMoviesTextStyles.current

    val spacing: MoviesSpacingTokens
        get() = MoviesSpacing.tokens

    val radii: MoviesRadiusTokens
        get() = ExpressiveRadii.forBuild

    val shapes: MoviesShapeTokens
        get() = ExpressiveShapes.forBuild

    val sizes: MoviesSizeTokens
        get() = MoviesSizes.tokens

    val strokes: MoviesStrokeTokens
        get() = MoviesStrokes.tokens

    val elevations: MoviesElevationTokens
        get() = MoviesElevations.tokens

    val effects: MoviesEffectTokens
        get() = MoviesEffects.tokens

    val motion: MoviesMotionTokens
        get() = MoviesMotion.tokens

    val focus: MoviesFocusTokens
        get() = MoviesFocus.tokens

    val focusRing: MoviesFocusRingStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalMoviesFocusRingStyle.current

    val layout: MoviesLayoutTokens
        get() = MoviesLayout.tokens

    val media: MoviesMediaTokens
        get() = MoviesMedia.tokens

    val components: MoviesComponentTokens
        get() = ExpressiveComponents.forBuild

    val currentTheme: AppTheme
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTheme.current

    val settingsUiStyle: SettingsUiStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalSettingsUiStyle.current

    val cardFocusScale: Float
        @Composable
        @ReadOnlyComposable
        get() = ExpressiveMotion.cardFocusScale(LocalAppTheme.current)
}
