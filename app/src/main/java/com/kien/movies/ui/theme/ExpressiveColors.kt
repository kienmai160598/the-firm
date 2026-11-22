package com.kien.movies.ui.theme

import androidx.compose.ui.graphics.Color

// A neutral cinema dark in the Apple TV manner: near black behind everything, Apple's dark
// system grays for raised surfaces, white for focus with black content on it, and translucent
// white ("frosted") for resting and selected controls. Netflix red is kept for progress, new or
// live badges and destructive emphasis only, so it never competes with focus.
internal object ExpressiveColors {
    val background = Color(0xFF0B0B0C)
    val systemGray6 = Color(0xFF1C1C1E)
    val systemGray5 = Color(0xFF2C2C2E)
    val systemGray4 = Color(0xFF3A3A3C)
    val systemGray2 = Color(0xFF636366)

    // Text and divider tones are white at 64%, 40% and 10% composited over [background], kept
    // opaque because call sites derive fainter tones with copy(alpha), which replaces alpha.
    val textPrimary = Color.White
    val textSecondary = Color(0xFFA7A7A7)
    val textTertiary = Color(0xFF6D6D6D)
    val divider = Color(0xFF232324)

    /** Resting fill of a button, pill or chip: white at 14% over whatever is behind it. */
    val frosted = Color.White.copy(alpha = 0.14f)

    /** A slightly stronger frosted fill, for a pressed control or a selected one on a panel. */
    val frostedStrong = Color.White.copy(alpha = 0.22f)

    /** Focused fill of a row whose text stays white: brighter frosted. */
    val frostedFocused = Color.White.copy(alpha = 0.30f)

    /** A pressed white control dims a touch. */
    val secondaryPressed = Color(0xFFD1D1D6)

    /** Focused fill: solid white, with [onFocus] content. */
    val focus = Color.White
    val onFocus = Color.Black

    /** Netflix red: progress bars, new or live badges, destructive emphasis. Nothing else. */
    val accentRed = Color(0xFFE50914)

    /** A translucent cinema bar or panel over artwork, such as the top tab bar. */
    val barScrim = systemGray6.copy(alpha = 0.80f)

    /** A dialog or side panel: dark frosted, nearly opaque so text over artwork stays legible. */
    val dialogPanel = systemGray6.copy(alpha = 0.94f)

    /** Soft drop shadow under a lifted card. */
    val liftShadow = Color.Black

    /** Second badge colour that must differ from the first: a dark neutral, not a hue. */
    val tertiaryContainer = systemGray4

    val Palette = ThemeColorPalette(
        secondary = focus,
        secondaryVariant = systemGray4,
        onSecondary = onFocus,
        onSecondaryVariant = textPrimary,
        focusRing = focus,
        focusBackground = frosted,
        background = background,
        backgroundElevated = systemGray6,
        backgroundCard = systemGray6,
        surface = systemGray6,
        surfaceVariant = systemGray5,
        panel = systemGray6,
        overlay = Color(0xD9000000),
        field = systemGray5,
        menu = systemGray5,
        modal = systemGray6,
        primary = textPrimary,
        onPrimary = onFocus,
        outline = systemGray2,
        outlineVariant = divider,
        textSecondary = textSecondary,
        textTertiary = textTertiary
    )
}
