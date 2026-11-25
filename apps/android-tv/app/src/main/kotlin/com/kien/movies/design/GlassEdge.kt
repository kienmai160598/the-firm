package com.kien.movies.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border

/**
 * The light rim Android draws where tvOS shows Liquid Glass (spec section 2, "Glass edge"): a 1 dp
 * stroke inside [shape] in [TvColors.glassEdge]. Pass `visible = false` once the control turns
 * solid white on focus; the edge is dropped then, as in the YouTube TV app.
 *
 * Use it on every surface that is glass on tvOS: sidebar panel, nav pill, circular buttons,
 * unfocused pills, platters, description panel. It is one stroke, no extra layer.
 */
fun Modifier.glassEdge(shape: Shape, visible: Boolean = true): Modifier = if (visible) border(GLASS_EDGE_WIDTH, TvColors.glassEdge, shape) else this

/**
 * The glass edge as a tv-material [Border], for clickable surfaces: drawn inside the surface's
 * focus scale, so it grows with the control. Pair it with `focusedBorder = Border.None`.
 */
fun glassEdgeBorder(shape: Shape): Border = Border(border = BorderStroke(GLASS_EDGE_WIDTH, TvColors.glassEdge), shape = shape)

/**
 * The rim of tvOS Liquid Glass over video, where the light catches the edge: bright at the top,
 * nearly gone at the middle, lit again at the bottom (measured on the tvOS player's circle buttons
 * in the Apple TV simulator). Still one 1 dp stroke, now with a vertical gradient.
 */
val liquidGlassRim: Brush = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.38f),
    0.5f to Color.White.copy(alpha = 0.06f),
    1f to Color.White.copy(alpha = 0.18f)
)

/** [liquidGlassRim] as a modifier, for a panel that is not clickable. */
fun Modifier.liquidGlassEdge(shape: Shape): Modifier = border(BorderStroke(GLASS_EDGE_WIDTH, liquidGlassRim), shape)

/** [liquidGlassRim] as a tv-material [Border]. Pair it with `focusedBorder = Border.None`. */
fun liquidGlassBorder(shape: Shape): Border = Border(border = BorderStroke(GLASS_EDGE_WIDTH, liquidGlassRim), shape = shape)

/**
 * The rim of a focused card, the stand-in for the tvOS `.highlight` hover effect: the light lands
 * on the top edge and fades down the sides. Measured on the focused Continue Watching card in the
 * Apple TV simulator: the top edge about 100 levels brighter than the art under it, the sides
 * about 20.
 */
val focusedCardRim: Brush = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.6f),
    0.35f to Color.White.copy(alpha = 0.12f),
    1f to Color.White.copy(alpha = 0.08f)
)

internal val GLASS_EDGE_WIDTH = 1.dp
