package com.kien.movies.ui.theme

import androidx.compose.ui.graphics.Color
import com.kien.movies.domain.model.AppTheme
import com.kien.movies.domain.model.CustomThemeColors

data class ThemeColorPalette(
    val secondary: Color,
    val secondaryVariant: Color,
    val onSecondary: Color = MoviesPrimitives.white,
    val onSecondaryVariant: Color = MoviesPrimitives.white,
    val accentGradient: List<Color> = listOf(secondary),
    val focusRing: Color,
    val focusRingGradient: List<Color> = listOf(focusRing),
    val focusBackground: Color,
    val background: Color = MoviesPrimitives.neutral950,
    val backgroundElevated: Color = MoviesPrimitives.neutral900,
    val backgroundCard: Color = MoviesPrimitives.neutral825,
    val surface: Color = MoviesPrimitives.neutral875,
    val surfaceVariant: Color = MoviesPrimitives.neutral800,
    val panel: Color = MoviesPrimitives.neutral900,
    val overlay: Color = Color(0xD9000000),
    val field: Color = MoviesPrimitives.neutral850,
    val menu: Color = MoviesPrimitives.neutral875,
    val modal: Color = MoviesPrimitives.neutral900,
    val primary: Color = MoviesPrimitives.neutral500,
    val onPrimary: Color = MoviesPrimitives.white,
    val outline: Color = MoviesPrimitives.neutral600,
    val outlineVariant: Color = MoviesPrimitives.neutral750,
    val playerOverlay: Color = Color(0xCC000000),
    val textSecondary: Color = MoviesPrimitives.neutral400,
    val textTertiary: Color = MoviesPrimitives.neutral600
)

object ThemeColors {
    val Crimson = ThemeColorPalette(
        secondary = MoviesPrimitives.red500,
        secondaryVariant = MoviesPrimitives.red600,
        focusRing = MoviesPrimitives.red300,
        focusBackground = Color(0xFF3D1A1A),
        backgroundCard = Color(0xFF241A1A)
    )

    val Ocean = ThemeColorPalette(
        secondary = MoviesPrimitives.blue500,
        secondaryVariant = MoviesPrimitives.blue700,
        focusRing = MoviesPrimitives.blue300,
        focusBackground = Color(0xFF1A2D3D),
        background = Color(0xFF0D0D0F),
        backgroundElevated = Color(0xFF1A1A1E),
        backgroundCard = Color(0xFF1A1F24)
    )

    val Violet = ThemeColorPalette(
        secondary = MoviesPrimitives.violet500,
        secondaryVariant = MoviesPrimitives.violet700,
        focusRing = MoviesPrimitives.violet300,
        focusBackground = Color(0xFF2D1A3D),
        background = Color(0xFF0D0D0F),
        backgroundElevated = Color(0xFF1A1A1E),
        backgroundCard = Color(0xFF1F1A24)
    )

    val Emerald = ThemeColorPalette(
        secondary = MoviesPrimitives.green500,
        secondaryVariant = MoviesPrimitives.green700,
        focusRing = MoviesPrimitives.green300,
        focusBackground = Color(0xFF1A3D1E),
        backgroundCard = Color(0xFF1A241A)
    )

    val Amber = ThemeColorPalette(
        secondary = MoviesPrimitives.amber500,
        secondaryVariant = MoviesPrimitives.amber700,
        focusRing = MoviesPrimitives.amber300,
        focusBackground = Color(0xFF3D2D1A),
        background = Color(0xFF0F0D0D),
        backgroundElevated = Color(0xFF1E1A1A),
        backgroundCard = Color(0xFF24201A)
    )

    val Rose = ThemeColorPalette(
        secondary = MoviesPrimitives.rose500,
        secondaryVariant = MoviesPrimitives.rose700,
        focusRing = MoviesPrimitives.rose300,
        focusBackground = Color(0xFF3D1A2D),
        backgroundCard = Color(0xFF241A1F)
    )

    val White = ThemeColorPalette(
        secondary = MoviesPrimitives.neutral100,
        secondaryVariant = MoviesPrimitives.neutral200,
        onSecondary = MoviesPrimitives.neutral925,
        onSecondaryVariant = MoviesPrimitives.neutral925,
        focusRing = MoviesPrimitives.white,
        focusBackground = Color(0xFF303030),
        backgroundCard = MoviesPrimitives.neutral850
    )

    fun getColorPalette(
        theme: AppTheme,
        customColors: CustomThemeColors = CustomThemeColors.Default
    ): ThemeColorPalette {
        return when (theme) {
            AppTheme.CUSTOM -> customColors.toColorPalette()
            AppTheme.GOLD -> SupporterThemeColors.Gold
            AppTheme.JADE -> SupporterThemeColors.Jade
            AppTheme.ROSE_GOLD -> SupporterThemeColors.RoseGold
            AppTheme.ARCTIC_BLUE -> SupporterThemeColors.ArcticBlue
            AppTheme.GRAPHITE -> SupporterThemeColors.Graphite
            AppTheme.CRIMSON -> Crimson
            AppTheme.OCEAN -> Ocean
            AppTheme.VIOLET -> Violet
            AppTheme.EMERALD -> Emerald
            AppTheme.AMBER -> Amber
            AppTheme.ROSE -> Rose
            AppTheme.WHITE -> White
            AppTheme.EXPRESSIVE -> ExpressiveColors.Palette
        }
    }
}
