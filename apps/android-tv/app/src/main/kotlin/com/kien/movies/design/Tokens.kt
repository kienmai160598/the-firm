package com.kien.movies.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Every size, colour and duration the screens use. Mirrors the literals of apps/apple-tv/Sources
// with dp = pt / 2 and sp = pt / 2 (docs/android-tv-rebuild.md, section 4). No literal sizes in
// screens: add a token here (through a request, this folder is frozen for the feature slices).

/** Insets and gaps. The canvas is 960 x 540 dp. */
object Spacing {
    /** Overscan floor and the side inset of hero and rows (tvOS 80 pt, Android floor 48 dp). */
    val screenHorizontal = 48.dp

    /** Overscan floor at the top and bottom. */
    val screenVertical = 27.dp

    /** Space between the cards of a shelf (tvOS 40 pt). */
    val shelfItem = 20.dp

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
    val cornerRadius = 12.dp
    val contentInset = 10.dp
}

/** Streams list. */
object StreamsSize {
    val rowHeight = 52.dp
    val resolutionBadge = DpSize(62.dp, 32.dp)
}

/** The left navigation drawer around the tab roots. */
object RailSize {
    /** Start of the rail; the item's own 16 dp padding puts the icon inside the 48 dp overscan. */
    val start = 36.dp

    /** Collapsed item width of tv-material's NavigationDrawerItem (CollapsedDrawerItemWidth). */
    val collapsedItem = 56.dp

    /** Where tab content starts so nothing sits under the closed rail. */
    val contentStart = start + collapsedItem + 24.dp

    val itemGap = 4.dp
}

/** Focus look: scale in graphicsLayer, one soft shadow, no ring (section 4). */
object Focus {
    const val cardScale = 1.08f
    const val buttonScale = 1.05f
    const val animationMillis = 150
    val shadowElevation = 12.dp
}

/** Colours. Flat fills only: no full screen gradient, no blur, no stacked translucent layers. */
object Palette {
    /** Window colour of Home and the tab roots (tvOS Home gradient midpoint). */
    val homeBackground = Color(0xFF24282E)

    /** Window colour of Details and Streams (tvOS Details gradient midpoint). */
    val detailsBackground = Color(0xFF1B1D22)

    /** The open drawer's panel: opaque, so it adds no translucent layer over the scrim. */
    val drawerPanel = Color(0xFF15171B)

    /** Scrim over content while the drawer is open (one layer). */
    val drawerScrim = Color(0x80000000)

    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.6f)
    val textTertiary = Color.White.copy(alpha = 0.45f)

    /** Placeholder behind artwork that has not loaded (tvOS .quaternary). */
    val artworkPlaceholder = Color(0xFF33373E)

    /** Flat frosted fills that replace tvOS glass: rest, raised, strong. */
    val frostRest = Color.White.copy(alpha = 0.14f)
    val frostRaised = Color.White.copy(alpha = 0.22f)
    val frostStrong = Color.White.copy(alpha = 0.30f)

    /** A focused control is solid white with black content. */
    val focusedContainer = Color.White
    val focusedContent = Color.Black

    val imdbYellow = Color(0xFFF5C518)
    val shadow = Color.Black
}

/** Durations copied from tvOS as they are. */
object Motion {
    const val heroAdvanceMillis = 8_000L
    const val controlsHideMillis = 5_000L
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
