package com.kien.movies.design

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The light rim Android draws where tvOS shows Liquid Glass (spec section 2, "Glass edge"): a 1 dp
 * stroke inside [shape] in [TvColors.glassEdge]. Pass `visible = false` once the control turns
 * solid white on focus; the edge is dropped then, as in the YouTube TV app.
 *
 * Use it on every surface that is glass on tvOS: sidebar panel, nav pill, circular buttons,
 * unfocused pills, platters, description panel. It is one stroke, no extra layer.
 */
fun Modifier.glassEdge(shape: Shape, visible: Boolean = true): Modifier = if (visible) border(GLASS_EDGE_WIDTH, TvColors.glassEdge, shape) else this

private val GLASS_EDGE_WIDTH = 1.dp
