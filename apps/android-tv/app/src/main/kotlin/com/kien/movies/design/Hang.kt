package com.kien.movies.design

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Apple's text alignment: the text inside a wrapper (a capsule, a platter, a field) starts on the
// same edge as the plain text of its column, and the wrapper hangs out past that edge by its own
// padding. Text stays inside the safe area; only the wrapper's background reaches past it.

/**
 * Hangs this wrapper [start] past the leading edge of its slot and [end] past the trailing edge,
 * so content inset by those paddings lines up with the slot's edges. The parent lays the wrapper
 * out as if it were [start] + [end] narrower, so the spacing to its siblings does not change. A
 * wrapper that fills its slot is widened by [start] + [end] to keep filling it.
 */
fun Modifier.hangOutside(start: Dp, end: Dp = 0.dp): Modifier = layout { measurable, constraints ->
    val startPx = start.roundToPx()
    val endPx = end.roundToPx()
    val extra = startPx + endPx
    val widened = if (constraints.hasBoundedWidth) {
        constraints.copy(
            minWidth = if (constraints.hasFixedWidth) constraints.maxWidth + extra else constraints.minWidth,
            maxWidth = constraints.maxWidth + extra
        )
    } else {
        constraints
    }
    val placeable = measurable.measure(widened)
    layout((placeable.width - extra).coerceAtLeast(0), placeable.height) {
        placeable.place(-startPx, 0)
    }
}
