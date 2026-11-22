package com.kien.movies.ui.screens.library

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import com.kien.movies.LocalContentFocusRequester
import com.kien.movies.ui.navigation.toptabbar.LocalTopTabBarInset
import com.kien.movies.ui.navigation.toptabbar.ReportTopTabBarScroll
import com.kien.movies.ui.navigation.toptabbar.isScrolledDown
import com.kien.movies.ui.navigation.toptabbar.topTabBarFloatsOver
import com.kien.movies.ui.theme.MoviesTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

/** Top padding of the library's header and messages: below the top tab bar if shown. */
@Composable
internal fun libraryTopPadding(): Dp = LocalTopTabBarInset.current + MoviesTheme.spacing.xl

@Composable
internal fun ReportLibraryGridScroll(gridState: LazyGridState) {
    ReportTopTabBarScroll { gridState.isScrolledDown() }
}

// The header rows are grid items, so a minimal bring-into-view can stop with the title row
// still scrolled off and the floating top bar would then slide in over the view mode tabs.
@Composable
internal fun Modifier.revealGridTopOnFocus(gridState: LazyGridState): Modifier {
    if (!topTabBarFloatsOver()) return this
    var hasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(gridState, hasFocus) {
        if (hasFocus) gridState.keepTopRevealed()
    }
    return onFocusChanged { focusState -> hasFocus = focusState.hasFocus }
}

// Down from the top tab bar lands on the view mode tabs. A directional move would reach the
// grid's focus restorer first, which has no card to offer while the library is empty.
@Composable
internal fun Modifier.enterFromTopTabBar(): Modifier {
    if (!topTabBarFloatsOver()) return this
    return focusRequester(LocalContentFocusRequester.current).focusRestorer()
}

// Focus runs its own bring-into-view scroll at the same priority, and whichever scroll starts
// last cancels the other, so scroll back up each time the grid settles below its top.
private suspend fun LazyGridState.keepTopRevealed() {
    while (true) {
        snapshotFlow { !isScrollInProgress && isScrolledDown() }
            .first { settledBelowTop -> settledBelowTop }
        scrollToTopUnlessPreempted()
    }
}

private suspend fun LazyGridState.scrollToTopUnlessPreempted() {
    try {
        animateScrollToItem(0)
    } catch (preempted: CancellationException) {
        currentCoroutineContext().ensureActive()
    }
}
