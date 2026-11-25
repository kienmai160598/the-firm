package com.kien.movies.design

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

// How a scroller brings a focused child into view (spec section 5, "Row scroll"). The TV default
// pulls every focused child to a pivot line; these keep the focused card where the reference does.
// Every spec scrolls on [Motion.focusSpring], with the focused control's lift, as tvOS moves both.

/** A [BringIntoViewSpec] that keeps a focused child between [leading] and [trailing] insets. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberInsetBringIntoViewSpec(leading: Dp, trailing: Dp = leading): BringIntoViewSpec {
    val density = LocalDensity.current
    return remember(density, leading, trailing) {
        val leadingPx = with(density) { leading.toPx() }
        val trailingPx = with(density) { trailing.toPx() }
        object : FocusPacedSpec() {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = insetScrollDistance(offset, size, containerSize, leadingPx, trailingPx)
        }
    }
}

/**
 * How far to scroll so a child at [offset] with [size] sits inside the container minus the
 * insets: zero when it already does, the least distance otherwise. A child larger than the space
 * is aligned to the leading inset.
 */
internal fun insetScrollDistance(offset: Float, size: Float, containerSize: Float, leading: Float, trailing: Float): Float {
    val end = containerSize - trailing
    return when {
        offset < leading -> offset - leading
        offset + size > end -> minOf(offset + size - end, offset - leading)
        else -> 0f
    }
}

/**
 * Keeps the focused item of a row at the row's start margin: the row slides, the focused card
 * stays in place. The last items stop where the row ends.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberStartAlignedRowSpec(leading: Dp = TvGrid.sideMargin): BringIntoViewSpec {
    val density = LocalDensity.current
    return remember(density, leading) {
        val leadingPx = with(density) { leading.toPx() }
        object : FocusPacedSpec() {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = offset - leadingPx
        }
    }
}

/**
 * No automatic scroll: for a page that scrolls each section to its own place when it takes focus
 * (Details), so the default bring into view must not move the page as well.
 */
@OptIn(ExperimentalFoundationApi::class)
val NoScrollSpec: BringIntoViewSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

/**
 * The platform's bring into view (the TV pivot) on [Motion.focusSpring]: provided at the root by
 * [MoviesTheme], so a list without its own spec scrolls at the tvOS pace too.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberFocusPacedPlatformSpec(): BringIntoViewSpec {
    val platform = LocalBringIntoViewSpec.current
    return remember(platform) {
        object : FocusPacedSpec() {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = platform.calculateScrollDistance(offset, size, containerSize)
        }
    }
}

/**
 * Scrolls on [Motion.focusSpring] instead of the platform's stiff default spring. The property is
 * deprecated but still read by the scroller in Compose foundation 1.13 (ContentInViewNode).
 */
@OptIn(ExperimentalFoundationApi::class)
private abstract class FocusPacedSpec : BringIntoViewSpec {
    @Suppress("OVERRIDE_DEPRECATION")
    override val scrollAnimationSpec: AnimationSpec<Float> = Motion.focusSpring
}
