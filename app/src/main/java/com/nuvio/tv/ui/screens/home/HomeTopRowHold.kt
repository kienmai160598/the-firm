package com.nuvio.tv.ui.screens.home

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.nuvio.tv.ui.navigation.toptabbar.scrollFromTop
import com.nuvio.tv.ui.navigation.toptabbar.topTabBarFloatsOver
import kotlin.math.roundToInt

/** Where a home page is scrolled, as its bring-into-view spec needs to see it. */
private class HomePageScroll(
    val scrollFromTop: () -> Int?,
    val viewportHeight: () -> Int
)

/** [pageSpec] for the page scrolled by [pageState], holding its top row at the very top. */
@Composable
internal fun rememberTopRowHold(
    pageSpec: BringIntoViewSpec,
    pageState: LazyListState
): BringIntoViewSpec {
    val pageScroll = remember(pageState) {
        HomePageScroll(pageState::scrollFromTop) { pageState.layoutInfo.viewportSize.height }
    }
    return rememberTopRowHold(pageSpec, pageScroll)
}

@Composable
internal fun rememberTopRowHold(
    pageSpec: BringIntoViewSpec,
    pageState: LazyGridState
): BringIntoViewSpec {
    val pageScroll = remember(pageState) {
        HomePageScroll(pageState::scrollFromTop) { pageState.layoutInfo.viewportSize.height }
    }
    return rememberTopRowHold(pageSpec, pageScroll)
}

@Composable
private fun rememberTopRowHold(
    pageSpec: BringIntoViewSpec,
    pageScroll: HomePageScroll
): BringIntoViewSpec {
    val holdDistance = if (topTabBarFloatsOver()) homeFirstRowTopPadding() else 0.dp
    val holdDistancePx = with(LocalDensity.current) { holdDistance.toPx() }
    return remember(pageSpec, pageScroll, holdDistancePx) {
        pageSpec.holdingTopRow(holdDistancePx, pageScroll)
    }
}

/**
 * Lets a page rest only at its top while its first row has focus. That row sits lower to clear
 * the top tab bar, so the page's spec would otherwise park it a little scrolled, under the bar,
 * both on entering it and on coming back up to it, and the bar would stay hidden.
 * The deprecated scrollAnimationSpec is still forwarded: Compose 1.11's ContentInViewNode reads it.
 */
@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
private fun BringIntoViewSpec.holdingTopRow(
    holdDistancePx: Float,
    pageScroll: HomePageScroll
): BringIntoViewSpec {
    if (holdDistancePx <= 0f) return this
    val pageSpec = this
    return object : BringIntoViewSpec {
        override val scrollAnimationSpec: AnimationSpec<Float> = pageSpec.scrollAnimationSpec

        override fun calculateScrollDistance(
            offset: Float,
            size: Float,
            containerSize: Float
        ): Float {
            val distance = pageSpec.calculateScrollDistance(offset, size, containerSize)
            // Rows nested in the page read this spec too; they pass their own width here.
            if (containerSize.roundToInt() != pageScroll.viewportHeight()) return distance
            val fromTop = pageScroll.scrollFromTop() ?: return distance
            val restingFromTop = fromTop + distance
            val restsNearTop = restingFromTop > 0f && restingFromTop <= holdDistancePx
            val holds = restsNearTop && fitsAtTop(offset + fromTop, size, containerSize)
            return if (holds) -fromTop.toFloat() else distance
        }
    }
}

private fun fitsAtTop(offsetAtTop: Float, size: Float, containerSize: Float): Boolean =
    offsetAtTop >= 0f && offsetAtTop + size <= containerSize
