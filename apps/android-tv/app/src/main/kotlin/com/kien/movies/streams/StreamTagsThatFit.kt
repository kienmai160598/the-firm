package com.kien.movies.streams

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * The checked row's tag line (tvOS StreamTagsThatFit): the tags left to right, as many as fit the
 * width, the rest left out, so a long status never pushes the tags out of the row. tvOS picks the
 * longest prefix with ViewThatFits; this places children while the next one still fits.
 */
@Composable
fun StreamTagsThatFit(gap: Dp, modifier: Modifier = Modifier, tags: @Composable () -> Unit) {
    Layout(content = tags, modifier = modifier) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val loose = Constraints(maxHeight = constraints.maxHeight)
        val placeables = measurables.map { it.measure(loose) }
        val xs = mutableListOf<Int>()
        var x = 0
        for (placeable in placeables) {
            val start = if (xs.isEmpty()) 0 else x + gapPx
            if (start + placeable.width > constraints.maxWidth) break
            xs += start
            x = start + placeable.width
        }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(width = x.coerceAtLeast(constraints.minWidth), height = height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            xs.forEachIndexed { index, start ->
                val placeable = placeables[index]
                placeable.placeRelative(start, (height - placeable.height) / 2)
            }
        }
    }
}
