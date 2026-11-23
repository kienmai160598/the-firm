// lead: move to design/
// Private copy of the shared look tokens (docs/tv-look-spec.md, sections 1 and 2) for Streams,
// the player and Addons, under the names the brief gives (TvGrid, glassEdge, TvType, TvColors,
// TvShapes). The player and Addons import them from here until design/ holds the shared ones.
// TvControls and TvGlass are extras of this copy: control sizes and focus scales, and the glass
// edge as a tv-material border parameter.
package com.kien.movies.streams

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.ClickableSurfaceShape
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.ListItemBorder
import androidx.tv.material3.ListItemDefaults

/** The column grid every shelf sits on: 960 x 540 dp canvas, 864 dp content, 20 dp gutter. */
object TvGrid {
    /** Side margin, the Android overscan floor (tvOS 80 pt). */
    val sideMargin = 48.dp

    /** Top and bottom overscan floor. */
    val verticalMargin = 27.dp

    val contentWidth = 864.dp
    val gutter = 20.dp

    /** First content line under the collapsed nav pill on a tab root (Search field top, 134 pt). */
    val tabContentTop = 67.dp

    /** Width of one item when [columns] items fill the content width. */
    fun itemWidth(columns: Int): Dp = (contentWidth - gutter * (columns - 1)) / columns
}

/** Spec section 2 Type: tvOS text styles at sp = pt / 2. Colour is the caller's. */
object TvType {
    /** Search field text, Title 2 (57 pt). Also the page title of Streams. */
    val searchField = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium, lineHeight = 33.sp)

    /** Empty state title, Headline bold (38 pt). */
    val headline = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 23.sp)

    /** Row title, Callout bold (31 pt), drawn in secondary. */
    val rowTitle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 19.sp)

    /** Nav pill label, result card title, button label: Body bold (29 pt). */
    val body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)

    /** Metadata line, sidebar item: Caption 1 (25 pt). */
    val caption1 = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)

    /** Overview, description, subtitle: Caption 2 (23 pt, leading 30). */
    val caption2 = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 15.sp)

    /** Overline and badge text, 19 pt uppercase: 11 sp floor. */
    val overline = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
}

/** Spec section 2 Colour, Android column. */
object TvColors {
    val homeRowsBackground = Color(0xFF1C2228)
    val detailsBackground = Color(0xFF141C24)
    val searchBackground = Color(0xFF2E3437)

    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.6f)
    val textTertiary = Color.White.copy(alpha = 0.35f)

    val focusedFill = Color(0xFFEBEBEA)
    val focusedContent = Color(0xFF141414)

    /** Unfocused pill or platter: one flat layer of white 22%, plus the glass edge. */
    val platter = Color.White.copy(alpha = 0.22f)

    /** Sidebar panel: near black at 96%, no blur, plus the glass edge. */
    val sidebarPanel = Color(0xF51A1A1A)

    /** The glass edge: 1 dp inside stroke, white 16%. */
    val glassEdge = Color.White.copy(alpha = 0.16f)
}

/** Spec section 2 Shape. */
object TvShapes {
    val card = RoundedCornerShape(8.dp)
    val panel = RoundedCornerShape(12.dp)
    val sidebar = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(percent = 50)
}

/** The 1 dp glass edge inside [shape]; dropped when the control turns solid white ([visible] false). */
fun Modifier.glassEdge(shape: Shape, visible: Boolean = true): Modifier = if (visible) border(GLASS_EDGE_WIDTH, TvColors.glassEdge, shape) else this

/** Control sizes and focus scales (spec section 2 and 3, Buttons and pills). */
object TvControls {
    /** Primary capsule on Details, 64 pt. */
    val pillHeight = 32.dp

    /** Circular secondary button, 64 pt. */
    val circle = 32.dp

    /** Glyph inside a pill or a circle. */
    val glyph = 16.dp

    const val cardFocusScale = 1.1f
    const val controlFocusScale = 1.05f

    /**
     * Full content width rows (stream list): 1.05 would push the focused edge 22 dp past the
     * 48 dp side margin and its text into the overscan band, so they grow 2% (17 dp in all).
     */
    const val wideRowFocusScale = 1.02f
}

/**
 * The glass look as tv-material parameters: platter fill at rest, solid white with dark content
 * when focused or pressed, and the glass edge only while not focused.
 */
object TvGlass {
    private val edge = Border(border = BorderStroke(GLASS_EDGE_WIDTH, TvColors.glassEdge))

    @Composable
    @ReadOnlyComposable
    fun buttonColors(): ButtonColors = ButtonDefaults.colors(
        containerColor = TvColors.platter,
        contentColor = TvColors.textPrimary,
        focusedContainerColor = TvColors.focusedFill,
        focusedContentColor = TvColors.focusedContent,
        pressedContainerColor = TvColors.focusedFill,
        pressedContentColor = TvColors.focusedContent,
        disabledContainerColor = TvColors.platter.copy(alpha = DISABLED_PLATTER_ALPHA),
        disabledContentColor = TvColors.textTertiary
    )

    fun pillShape(): ButtonShape = ButtonDefaults.shape(shape = TvShapes.pill)

    fun buttonScale(): ButtonScale = ButtonDefaults.scale(focusedScale = TvControls.controlFocusScale)

    @Composable
    @ReadOnlyComposable
    fun buttonBorder(): ButtonBorder = ButtonDefaults.border(
        border = edge.withShape(TvShapes.pill),
        focusedBorder = Border.None,
        pressedBorder = Border.None,
        disabledBorder = edge.withShape(TvShapes.pill),
        focusedDisabledBorder = edge.withShape(TvShapes.pill)
    )

    @Composable
    @ReadOnlyComposable
    fun surfaceColors(): ClickableSurfaceColors = ClickableSurfaceDefaults.colors(
        containerColor = TvColors.platter,
        contentColor = TvColors.textPrimary,
        focusedContainerColor = TvColors.focusedFill,
        focusedContentColor = TvColors.focusedContent,
        pressedContainerColor = TvColors.focusedFill,
        pressedContentColor = TvColors.focusedContent
    )

    @Composable
    @ReadOnlyComposable
    fun surfaceShape(shape: Shape): ClickableSurfaceShape = ClickableSurfaceDefaults.shape(shape = shape)

    fun surfaceScale(focusedScale: Float = TvControls.controlFocusScale): ClickableSurfaceScale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale)

    @Composable
    @ReadOnlyComposable
    fun surfaceBorder(shape: Shape): ClickableSurfaceBorder = ClickableSurfaceDefaults.border(
        border = edge.withShape(shape),
        focusedBorder = Border.None,
        pressedBorder = Border.None
    )

    @Composable
    @ReadOnlyComposable
    fun listItemBorder(shape: Shape): ListItemBorder = ListItemDefaults.border(
        border = edge.withShape(shape),
        focusedBorder = Border.None,
        pressedBorder = Border.None
    )

    /** The glass edge of a circular tv-material `IconButton`. */
    @Composable
    @ReadOnlyComposable
    fun circleBorder(): ButtonBorder = IconButtonDefaults.border(
        border = edge.withShape(CircleShape),
        focusedBorder = Border.None,
        pressedBorder = Border.None,
        disabledBorder = edge.withShape(CircleShape),
        focusedDisabledBorder = edge.withShape(CircleShape)
    )

    fun circleShape(): ButtonShape = IconButtonDefaults.shape(shape = CircleShape)

    fun circleScale(): ButtonScale = IconButtonDefaults.scale(focusedScale = TvControls.controlFocusScale)

    private fun Border.withShape(shape: Shape): Border = copy(shape = shape)
}

/** White 22% composited over black once: the platter as an opaque colour, for use over video. */
val PlatterOverBlack: Color = TvColors.platter.compositeOver(Color.Black)

private val GLASS_EDGE_WIDTH = 1.dp
private const val DISABLED_PLATTER_ALPHA = 0.5f
