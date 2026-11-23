package com.kien.movies.ui.screens.home

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kien.movies.ui.navigation.toptabbar.LocalTopTabBarInset
import com.kien.movies.ui.navigation.toptabbar.ReportTopTabBarScroll
import com.kien.movies.ui.navigation.toptabbar.isScrolledDown
import com.kien.movies.ui.navigation.toptabbar.topTabBarFloatsOver
import com.kien.movies.ui.theme.MoviesTheme

private val HeroLogoMaxWidth = 220.dp

/** Top padding of a home page whose first row starts at the top: below the top tab bar if shown. */
@Composable
internal fun homeFirstRowTopPadding(): Dp = LocalTopTabBarInset.current + MoviesTheme.spacing.xl

@Composable
internal fun ReportHomePageScroll(pageState: LazyListState) {
    ReportTopTabBarScroll { pageState.isScrolledDown() }
}

@Composable
internal fun ReportHomePageScroll(pageState: LazyGridState) {
    ReportTopTabBarScroll { pageState.isScrolledDown() }
}

// The hero stays put and the rows scroll below it, behind it at the top: the first row stays the
// first visible item until it has fully passed under the hero, a whole row after it left view. So
// the page counts as scrolled down once the first row moved up further than the bar is tall;
// focusing it only moves the rows a few px.
@Composable
internal fun ReportModernRowsScroll(rowListState: LazyListState) {
    val scrolledDownPx = with(LocalDensity.current) { LocalTopTabBarInset.current.roundToPx() }
    ReportTopTabBarScroll {
        rowListState.firstVisibleItemIndex > 0 ||
            rowListState.firstVisibleItemScrollOffset > scrolledDownPx
    }
}

/** Up from the first row moves focus to the top tab bar instead of expanding that row. */
@Composable
internal fun upFromFirstRowGoesToTopTabBar(): Boolean = topTabBarFloatsOver()

// The section header would cover the top tab bar at the grid's top, so it waits for a scroll.
@Composable
internal fun rememberGridSectionHeaderWithoutHero(gridState: LazyGridState): State<Boolean> {
    val waitsForScroll = topTabBarFloatsOver()
    return remember(gridState, waitsForScroll) {
        derivedStateOf { !waitsForScroll || gridState.isScrolledDown() }
    }
}

// A full hero block grows up to the top tab bar's row, so a title wider than the logo slot would
// run under the bar; the logo never does because it stops left of it.
@Composable
internal fun Modifier.heroTitleWidthUnderTopBar(): Modifier = if (topTabBarFloatsOver()) widthIn(max = HeroLogoMaxWidth) else this
