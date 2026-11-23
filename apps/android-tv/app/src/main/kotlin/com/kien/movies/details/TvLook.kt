package com.kien.movies.details

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Private copies of the shared look tokens of docs/tv-look-spec.md (sections 1 and 2), named as
// the brief names them so the lead can merge them into design/.

// lead: move to design/
/** The column grid every shelf sits on (spec section 1): content 864 dp, gutter 20 dp. */
internal object TvGrid {
    val margin: Dp = 48.dp
    val contentWidth: Dp = 864.dp
    val gutter: Dp = 20.dp

    /** Width of one item when [columns] items fill the content width. */
    fun itemWidth(columns: Int): Dp = (contentWidth - gutter * (columns - 1)) / columns

    /** Distance from the content's left edge to the start of column [index] (0 based). */
    fun columnStart(columns: Int, index: Int): Dp = (itemWidth(columns) + gutter) * index
}

// lead: move to design/

/** Spec section 2 colours. */
internal object TvColors {
    val detailsBackground = Color(0xFF141C24)
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.6f)
    val textTertiary = Color.White.copy(alpha = 0.35f)
    val focusedFill = Color(0xFFEBEBEA)
    val focusedContent = Color(0xFF141414)
    val platter = Color.White.copy(alpha = 0.22f)
    val glassEdge = Color.White.copy(alpha = 0.16f)
    val focusShadow = Color.Black.copy(alpha = 0.4f)
}

// lead: move to design/

/** Spec section 2 text roles, sp = pt / 2. */
internal object TvType {
    val searchField = TextStyle(fontSize = 28.sp, lineHeight = 34.sp)
    val headline = TextStyle(fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold)
    val rowTitle = TextStyle(fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 18.sp)
    val caption1 = TextStyle(fontSize = 13.sp, lineHeight = 16.sp)
    val caption2 = TextStyle(fontSize = 12.sp, lineHeight = 15.sp)
    val overline = TextStyle(fontSize = 11.sp, lineHeight = 14.sp)
}

// lead: move to design/

/** Spec section 2 corners. */
internal object TvShapes {
    val card = RoundedCornerShape(8.dp)
    val panel = RoundedCornerShape(12.dp)
    val sidebar = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(50)
}

// lead: move to design/

/**
 * The light rim Android draws where tvOS shows Liquid Glass: a 1 dp stroke inside [shape], white
 * 16%. Pass `visible = false` when the control turns solid white on focus. Drawn in the draw
 * phase only: no layer, no recomposition.
 */
internal fun Modifier.glassEdge(shape: Shape, visible: Boolean): Modifier = if (!visible) {
    this
} else {
    drawWithContent {
        drawContent()
        val stroke = 1.dp.toPx()
        val half = stroke / 2
        val outline = shape.createOutline(Size(size.width - stroke, size.height - stroke), layoutDirection, this)
        translate(left = half, top = half) {
            drawOutline(outline, color = TvColors.glassEdge, style = Stroke(width = stroke))
        }
    }
}

/** Focus scales of the spec: cards 1.1, controls and platters 1.05. */
internal object TvFocus {
    const val cardScale = 1.1f
    const val controlScale = 1.05f
    val shadowElevation = 12.dp
}

/**
 * Keeps the focused item of a row at the row's start margin (spec section 5, row scroll): the row
 * slides, the focused card stays in place. The last items stop where the row ends.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberStartAlignedRowSpec(leading: Dp = TvGrid.margin): BringIntoViewSpec {
    val density = LocalDensity.current
    return remember(density, leading) {
        val leadingPx = with(density) { leading.toPx() }
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = offset - leadingPx
        }
    }
}

/**
 * No automatic vertical scroll: Details scrolls each section to its own place when it takes focus
 * (see `DetailsScroll`), so the default bring into view must not move the page as well.
 */
@OptIn(ExperimentalFoundationApi::class)
internal val NoScrollSpec: BringIntoViewSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}
