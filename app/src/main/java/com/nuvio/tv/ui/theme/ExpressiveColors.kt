package com.nuvio.tv.ui.theme

import androidx.compose.ui.graphics.Color

// Material 3 Tonal Spot dark scheme from the M3 baseline seed #6750A4, the same scheme the box's
// launcher builds (material-color-utilities 1.12.0, SchemeTonalSpot, contrast 0), so the app and
// Home share one palette. Nuvio's accent is M3's primary; its layers are M3's surface containers.
object ExpressiveColors {
    private val primary = Color(0xFFCFBDFE)
    private val onPrimary = Color(0xFF36275D)
    private val primaryContainer = Color(0xFF4D3D75)
    private val onPrimaryContainer = Color(0xFFE9DDFF)
    private val secondaryContainer = Color(0xFF4A4458)
    private val surface = Color(0xFF141218)
    private val surfaceContainerLow = Color(0xFF1D1B20)
    private val surfaceContainer = Color(0xFF211F24)
    private val surfaceContainerHigh = Color(0xFF2B292F)
    private val surfaceContainerHighest = Color(0xFF36343A)

    val Palette = ThemeColorPalette(
        secondary = primary,
        secondaryVariant = primaryContainer,
        onSecondary = onPrimary,
        onSecondaryVariant = onPrimaryContainer,
        focusRing = primary,
        focusBackground = secondaryContainer,
        background = surface,
        backgroundElevated = surfaceContainer,
        backgroundCard = surfaceContainerHigh,
        surface = surfaceContainer,
        surfaceVariant = surfaceContainerHighest,
        panel = surfaceContainerLow,
        field = surfaceContainerHighest,
        menu = surfaceContainerHigh,
        modal = surfaceContainer
    )
}
