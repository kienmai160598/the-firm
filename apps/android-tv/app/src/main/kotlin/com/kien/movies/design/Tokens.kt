package com.kien.movies.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Every size, colour and duration the screens use, dp = pt / 2 and sp = pt / 2. The Apple TV look
// (docs/tv-look-spec.md) lives in TvTokens.kt: TvGrid, TvColors, TvType, TvShapes. The objects here
// predate it; their values follow the spec where it names them, so older screens pick up the look.

/** Insets and gaps. The canvas is 960 x 540 dp. */
object Spacing {
    /** Overscan floor and the side inset of hero and rows (tvOS 80 pt, Android floor 48 dp). */
    val screenHorizontal = TvGrid.sideMargin

    /** Overscan floor at the top and bottom. */
    val screenVertical = 27.dp

    /** Space between the cards of a shelf (tvOS 40 pt). */
    val shelfItem = TvGrid.gutter

    /** Vertical padding of a shelf's card row, room for the focused scale (tvOS 34 pt). */
    val shelfRowPadding = 17.dp

    /** Gap between two shelves (tvOS 24 pt). */
    val shelfGap = 12.dp

    /** Width of the hero text block (tvOS 860 pt). */
    val heroTextWidth = 430.dp

    /** How much of the next shelf shows under the hero (tvOS 150 pt). */
    val nextShelfPeek = 75.dp

    /** Small gaps inside a text block. */
    val textGapSmall = 4.dp
    val textGap = 8.dp
    val textGapLarge = 12.dp
}

/** The one card shape of posters, episodes and Continue Watching (tvOS CardShape.swift, 6ad37b6a9f). */
object CardSize {
    val poster = DpSize(130.dp, 195.dp)
    val wide = DpSize(200.dp, 112.dp)
    val castPortrait = 90.dp

    /** Card corner (spec: tvOS 16 pt, Android 8 dp); [TvShapes.card]. */
    val cornerRadius = 8.dp
    val contentInset = 10.dp
}

/** Streams list. */
object StreamsSize {
    val rowHeight = 52.dp
    val resolutionBadge = DpSize(62.dp, 32.dp)
}

/** Focus look (spec section 2): scale in graphicsLayer, one soft shadow, no ring. */
object Focus {
    const val cardScale = 1.1f
    const val buttonScale = 1.05f

    /**
     * Wide rows (stream list, Addons installed list): 1.05 would push the focused edge past the
     * 48 dp side margin into the overscan band and eat the 20 dp gutter, so they grow 2%.
     */
    const val wideRowScale = 1.02f
    const val animationMillis = 150

    /** One soft shadow, 12 dp blur equivalent, in [TvColors.focusShadow]. */
    val shadowElevation = 12.dp
}

/** Colours. Flat fills only: no full screen gradient, no blur, no stacked translucent layers. */
object Palette {
    /** Window colour of Home and the tab roots: [TvColors.homeRows]. */
    val homeBackground = TvColors.homeRows

    /** Window colour of Details and Streams: [TvColors.details]. */
    val detailsBackground = TvColors.details

    /** The open drawer's panel: [TvColors.sidebarPanel]. */
    val drawerPanel = TvColors.sidebarPanel

    val textPrimary = TvColors.textPrimary
    val textSecondary = TvColors.textSecondary
    val textTertiary = TvColors.textTertiary

    /** Placeholder behind artwork that has not loaded. */
    val artworkPlaceholder = TvColors.artworkPlaceholder

    /** Flat frosted fills that replace tvOS glass: rest, raised, strong. */
    val frostRest = Color.White.copy(alpha = 0.14f)
    val frostRaised = Color.White.copy(alpha = 0.22f)
    val frostStrong = Color.White.copy(alpha = 0.30f)

    /** A focused control is solid white with dark content (spec: #EBEBEA, #141414). */
    val focusedContainer = TvColors.focusedFill
    val focusedContent = TvColors.focusedContent

    val imdbYellow = Color(0xFFF5C518)
    val shadow = Color.Black
}

/** Durations copied from tvOS as they are. */
object Motion {
    const val heroAdvanceMillis = 8_000L
    const val controlsHideMillis = 5_000L

    /**
     * Push and pop cross fade, timed on a simulator recording of the tvOS app: Settings to About
     * about 300 ms, Back about 200 ms, ease in and out.
     */
    const val pushMillis = 300
    const val popMillis = 220
}

/** Text roles. tvOS point sizes in the comments. */
object TextStyles {
    /** Hero title, 76 pt bold. */
    val heroTitle = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp)

    /** Details title, 80 pt bold. */
    val detailsTitle = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, lineHeight = 46.sp)

    /** Player title, 54 pt. */
    val playerTitle = TextStyle(fontSize = 27.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp)

    /** Streams title, 52 pt bold. */
    val streamsTitle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)

    /** Shelf header, 31 pt semibold. */
    val shelfHeader = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)

    /** Body, 29 pt. */
    val body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)

    /** Card title, 26 pt medium. */
    val cardTitle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)

    /** Small text, 19 to 25 pt: 12 sp floor. */
    val small = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp)

    /** Inside badges only, 19 pt bold. */
    val badge = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)

    /** Button label. */
    val button = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
}
