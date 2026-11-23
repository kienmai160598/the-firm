package com.kien.movies.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
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

internal val GLASS_EDGE_WIDTH = 1.dp
