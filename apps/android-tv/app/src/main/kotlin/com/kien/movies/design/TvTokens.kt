package com.kien.movies.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The Apple TV look in numbers (docs/tv-look-spec.md, sections 1 and 2): dp = pt / 2, sp = pt / 2.
// A value marked "measured" was taken from the reference captures because the spec has none or
// its value disagreed with the capture; the lead folds those back into the spec.

/**
 * The one column grid every shelf sits on (spec section 1): content width 864 dp between the 48 dp
 * side margins, 20 dp between items. Screens hold a column count and an aspect ratio; the width is
 * computed here and stays fractional, so n items and n - 1 gutters add up to the content width.
 */
object TvGrid {
    /** Side margin of every screen (tvOS 80 pt; Android overscan floor 48 dp). */
    val sideMargin: Dp = 48.dp

    /** Width between the side margins (tvOS 1760 pt). */
    val contentWidth: Dp = 864.dp

    /** Space between two items of a shelf (tvOS 40 pt). */
    val gutter: Dp = 20.dp

    /** Width of one item when [columns] items fill the content width: 3 -> 274.7, 4 -> 201, 5 -> 156.8, 6 -> 127.3 dp. */
    fun itemWidth(columns: Int): Dp {
        require(columns > 0) { "columns must be positive, was $columns" }
        return (contentWidth - gutter * (columns - 1)) / columns
    }

    /** Distance from the content's left edge to the start of column [index] (0 based) of [columns]. */
    fun columnStart(columns: Int, index: Int): Dp = (itemWidth(columns) + gutter) * index
}

/** Vertical insets of the 960 x 540 dp canvas. */
object TvInsets {
    /** Top and bottom overscan floor (Android TV guide: 27 dp). */
    val verticalMargin: Dp = 27.dp

    /**
     * First content line under the collapsed nav pill on a tab root: the Search field's magnifier
     * top, 134 pt on 39-search-results.png (the pill spans 50 to 110 pt).
     */
    val tabContentTop: Dp = 67.dp
}

/** Colours of spec section 2, Android column. Flat fills only. */
object TvColors {
    /**
     * Home under the rows, flat: the midpoint of the tvOS app's Home gradient, `#2B3038` at the
     * top to `#1C1F24` at the bottom (`HomeBackground` in HomeView.swift; decision table in
     * docs/android-tv-rebuild.md, "Not ported literally"). The hero art fades into it.
     */
    val homeRows = Color(0xFF24282E)

    /** Details background (navy). */
    val details = Color(0xFF141C24)

    /** Search and Library background (grey slate). */
    val searchLibrary = Color(0xFF2E3437)

    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.6f)
    val textTertiary = Color.White.copy(alpha = 0.35f)

    /** A focused control is filled with this and draws its content in [focusedContent]. */
    val focusedFill = Color(0xFFEBEBEA)
    val focusedContent = Color(0xFF141414)

    /** Unfocused pill, platter or circular button: one flat layer, plus [glassEdge]. */
    val platter = Color.White.copy(alpha = 0.22f)

    /**
     * The open sidebar panel: one layer, no blur, plus the liquid glass rim. Light enough that the
     * screen behind still shows through, as tvOS glass does.
     */
    val sidebarPanel = Color(0xFF1A1A1A).copy(alpha = 0.78f)

    /** The light 1 dp rim drawn where tvOS shows Liquid Glass. */
    val glassEdge = Color.White.copy(alpha = 0.16f)

    /** Collapsed nav pill. Measured `#191919` over black on `05-up-hero.png` (white about 10%). */
    val navPill = Color.White.copy(alpha = 0.10f)

    /** The circle behind the tab icon in the nav pill. Measured `#474747` over black (white about 28%). */
    val navPillIcon = Color.White.copy(alpha = 0.28f)

    /** The current tab's capsule in the open panel while focus is on another row (grey capsule). */
    val sidebarCurrent = Color.White.copy(alpha = 0.16f)

    /** Card focus shadow: black 40%. */
    val focusShadow = Color.Black.copy(alpha = 0.4f)

    /**
     * Player glass over video, measured on the tvOS player in the simulator: an unfocused circle
     * button darkens the video under it by about 26% (no white tint), and the track is grey
     * `#8F8F8F` at 63%. Both are one flat layer; the rim is [liquidGlassRim].
     */
    val playerGlass = Color.Black.copy(alpha = 0.26f)
    val playerTrack = Color(0xFF8F8F8F).copy(alpha = 0.63f)

    /** Behind artwork that has not loaded yet. */
    val artworkPlaceholder = Color(0xFF3F4446)
}

/**
 * Text roles of spec section 2 (tvOS HIG text styles, sp = pt / 2, leading / 2). The tvOS regular
 * weight is Medium and "emphasized" is Bold; the bold roles are the ones the spec lists as bold.
 */
object TvType {
    /** Search field text and placeholder: Title 2, 57 pt. */
    val searchField = TextStyle(fontSize = 28.sp, lineHeight = 33.sp, fontWeight = FontWeight.Medium)

    /** Page title of a pushed screen without a hero (Streams): Title 2, 57 pt, bold. */
    val pageTitle = searchField.copy(fontWeight = FontWeight.Bold)

    /** Empty state title: Headline 38 pt bold. */
    val headline = TextStyle(fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold)

    /** Row title ("Continue Watching"): Callout 31 pt bold, drawn in [TvColors.textSecondary]. */
    val rowTitle = TextStyle(fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold)

    /** Body 29 pt. */
    val body = TextStyle(fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)

    /** Body 29 pt bold: nav pill label, result card title, button label. */
    val bodyEmphasized = body.copy(fontWeight = FontWeight.Bold)

    /** Caption 1, 25 pt: metadata line, sidebar item. Bold for an episode title. */
    val caption1 = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)

    /** Caption 1 bold: episode title, panel headers. */
    val caption1Emphasized = caption1.copy(fontWeight = FontWeight.Bold)

    /** Caption 2, 23 pt, leading 30: overview, descriptions, subtitles. */
    val caption2 = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)

    /** Overline ("EPISODE 2") and price caption, 19 pt uppercase; 11 sp is the badge floor. */
    val overline = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
}

/** Corners of spec section 2. */
object TvShapes {
    /** Poster, 16:9 card, Continue Watching (tvOS 16 pt continuous). */
    val card = RoundedCornerShape(8.dp)

    /** Result card, description panel (tvOS 24 pt). */
    val panel = RoundedCornerShape(12.dp)

    /** Open sidebar panel (tvOS 32 pt). */
    val sidebar = RoundedCornerShape(16.dp)

    /** Buttons, nav pill, season tab, suggestion: a capsule. */
    val pill = RoundedCornerShape(50)
}

/** Control sizes (spec section 3, Buttons and pills). Focus scales are in [Focus]. */
object TvControls {
    /** Primary capsule on Details and in panels and dialogs, 64 pt. */
    val pillHeight: Dp = 32.dp

    /**
     * Circular control of the player's transport row, 64 pt. The player is not captured (spec
     * section 4); the hero's circular buttons are 66 pt (33 dp) and set their own size.
     */
    val circle: Dp = 32.dp

    /** Glyph inside a pill or a circle. */
    val glyph: Dp = 16.dp
}
