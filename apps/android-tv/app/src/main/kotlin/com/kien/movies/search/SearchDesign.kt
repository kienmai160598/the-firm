package com.kien.movies.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border

// Private copies of the shared look tokens of docs/tv-look-spec.md, under the names the brief gives
// so the lead can merge them with design/. Search and the empty state use only these, not the
// older Tokens.kt values (card 12 dp, focus 1.08), which predate the spec.

/** The one column grid of every shelf (spec section 1). lead: move to design/ */
object TvGrid {
    /** Side margin: the Android overscan floor (tvOS 80 pt). */
    val margin = 48.dp

    /** Content width between the margins (tvOS 1760 pt). */
    val contentWidth = 864.dp

    /** Space between two items of a shelf (tvOS 40 pt). */
    val gutter = 20.dp

    /** Width of one item when [columns] items fill the content width. */
    fun itemWidth(columns: Int): Dp = (contentWidth - gutter * (columns - 1)) / columns
}

/** Spec section 2, colour. lead: move to design/ */
object TvColors {
    /** Search and Library background (tvOS gradient #273137 to #35393A, flat on Android). */
    val searchBackground = Color(0xFF2E3437)

    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.6f)
    val textTertiary = Color.White.copy(alpha = 0.35f)

    /** A focused control turns solid: this fill, [focusedContent] on it. */
    val focusedFill = Color(0xFFEBEBEA)
    val focusedContent = Color(0xFF141414)

    /** Unfocused pill or card platter: one flat layer. */
    val platter = Color.White.copy(alpha = 0.22f)

    /** The 1 dp rim that stands in for Liquid Glass. */
    val glassEdge = Color.White.copy(alpha = 0.16f)
}

/** Spec section 2, type (sp = pt / 2). lead: move to design/ */
object TvType {
    /** Search field text and placeholder, Title 2 57 pt. */
    val searchField = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium, lineHeight = 33.sp)

    /** Empty state title, Headline 38 pt bold. */
    val headline = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 23.sp)

    /** Row title ("Top Results"), Callout 31 pt bold, drawn in secondary. */
    val rowTitle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)

    /** Result card title and nav pill label, Body 29 pt bold. */
    val body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)

    /** Metadata line, Caption 1 25 pt. */
    val caption1 = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)

    /** Overview and result subtitle, Caption 2 23 pt, leading 30. */
    val caption2 = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 15.sp)

    /** Overline and badges, 19 pt uppercase; 11 sp is the floor. */
    val overline = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp)
}

/** Spec section 2, shape. lead: move to design/ */
object TvShapes {
    val card = RoundedCornerShape(8.dp)
    val panel = RoundedCornerShape(12.dp)
    val sidebar = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(50)
}

/**
 * The glass edge: a 1 dp stroke inside [shape], white 16%, on every surface that is Liquid Glass on
 * tvOS. Pass `visible = false` when the control turns solid white on focus. lead: move to design/
 */
fun Modifier.glassEdge(shape: Shape, visible: Boolean): Modifier = if (visible) border(GLASS_EDGE_WIDTH, TvColors.glassEdge, shape) else this

/**
 * The glass edge as a tv-material [Border], for clickable surfaces: drawn inside the surface's
 * focus scale, so it grows with the control. lead: move to design/
 */
fun glassEdgeBorder(shape: Shape): Border = Border(border = BorderStroke(GLASS_EDGE_WIDTH, TvColors.glassEdge), shape = shape)

private val GLASS_EDGE_WIDTH = 1.dp
