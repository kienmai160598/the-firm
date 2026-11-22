package com.kien.movies.ui.navigation.toptabbar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.kien.movies.ui.theme.ExpressiveMotion
import com.kien.movies.ui.theme.NuvioTheme
import kotlinx.coroutines.launch

/** Where a tab sits in its row, in pixels. */
@Immutable
internal data class TopTabSpan(val left: Float, val width: Float)

private val FocusGrowth = 2.dp
private const val PRESSED_CORNER_FRACTION = 0.3f
private const val ROUND_CORNER_FRACTION = 0.5f

/**
 * The tvOS tab indicator: one capsule that slides and resizes to [target] on the expressive
 * default spatial spring. While the bar holds focus it turns solid white and grows a little on
 * the focus lift spring; otherwise it marks the selected tab with a frosted fill. A press
 * squares its corners. Positions are read in the draw phase, so a slide recomposes nothing.
 */
@Composable
internal fun TopTabIndicator(
    target: TopTabSpan?,
    barFocused: Boolean,
    pressed: Boolean,
    modifier: Modifier = Modifier
) {
    val left = remember { Animatable(0f) }
    val width = remember { Animatable(0f) }

    LaunchedEffect(target) {
        if (target == null) return@LaunchedEffect
        if (width.value == 0f) {
            left.snapTo(target.left)
            width.snapTo(target.width)
            return@LaunchedEffect
        }
        launch { left.animateTo(target.left, ExpressiveMotion.expressiveDefaultSpatial()) }
        launch { width.animateTo(target.width, ExpressiveMotion.expressiveDefaultSpatial()) }
    }

    val color = animateColorAsState(
        targetValue = if (barFocused) {
            NuvioTheme.colors.Secondary
        } else {
            NuvioTheme.colors.FocusBackground
        },
        animationSpec = ExpressiveMotion.defaultEffects(),
        label = "topTabIndicatorColor"
    )
    val growth = animateFloatAsState(
        targetValue = if (barFocused) 1f else 0f,
        animationSpec = ExpressiveMotion.focusLift(),
        label = "topTabIndicatorGrowth"
    )
    val cornerFraction = animateFloatAsState(
        targetValue = if (pressed) PRESSED_CORNER_FRACTION else ROUND_CORNER_FRACTION,
        animationSpec = ExpressiveMotion.focusLift(),
        label = "topTabIndicatorCorner"
    )

    Box(
        modifier = modifier.drawBehind {
            if (width.value == 0f) return@drawBehind
            val grow = FocusGrowth.toPx() * growth.value
            val pillHeight = TopTabHeight.toPx() + grow * 2
            val pillSize = Size(width.value + grow * 2, pillHeight)
            val corner = pillHeight * cornerFraction.value
            drawRoundRect(
                color = color.value,
                topLeft = Offset(left.value - grow, (size.height - pillHeight) / 2),
                size = pillSize,
                cornerRadius = CornerRadius(corner, corner)
            )
        }
    )
}
