package com.kien.movies.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp

// Apple's concentric corner rule: outer radius = inner radius + padding. A rounded shape nested in
// another one, with an even inset all round, shares its centre of curvature, so the gap between
// the two edges stays the same width around the corner. Derive one radius from the other with
// these two functions instead of picking two unrelated corners.

/**
 * The radius of a shape nested [inset] inside a shape of radius [outer] (Apple's concentric rule,
 * inner = outer - inset). When the inset is larger than the outer radius the rule gives 0 or
 * less: the inner shape then takes [minimum], like SwiftUI's corner style `.concentric(minimum:)`
 * on a `ConcentricRectangle`. The result is never below 0.
 */
fun concentricRadius(outer: Dp, inset: Dp, minimum: Dp = 0.dp): Dp = (outer - inset).coerceAtLeast(minimum.coerceAtLeast(0.dp))

/**
 * The radius of a container that holds a shape of radius [inner] at [padding] from its edge
 * (Apple's concentric rule, outer = inner + padding). Never below 0.
 */
fun containerRadius(inner: Dp, padding: Dp): Dp = (inner + padding).coerceAtLeast(0.dp)
