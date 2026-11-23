package com.kien.movies.ui.navigation.toptabbar

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.distinctUntilChanged

/** Which root screens are scrolled away from their top; the top tab bar hides over those. */
internal class TopTabBarScrollRegistry {
    private val scrolledDownRoutes = mutableStateMapOf<String, Boolean>()

    fun isScrolledDown(route: String?): Boolean = scrolledDownRoutes[route] == true

    fun report(route: String, scrolledDown: Boolean) {
        scrolledDownRoutes[route] = scrolledDown
    }
}

internal val LocalTopTabBarScrollRegistry =
    staticCompositionLocalOf<TopTabBarScrollRegistry?> { null }

private val LocalTopTabBarScrollReporter = staticCompositionLocalOf<((Boolean) -> Unit)?> { null }

/** Lets [content], a root destination, report its scroll under [route]. */
@Composable
internal fun ProvideTopTabBarScrollReporter(route: String, content: @Composable () -> Unit) {
    val registry = LocalTopTabBarScrollRegistry.current
    val reporter = remember(registry, route) {
        registry?.let { { scrolledDown: Boolean -> it.report(route, scrolledDown) } }
    }
    CompositionLocalProvider(LocalTopTabBarScrollReporter provides reporter, content = content)
}

/**
 * Lets the floating top tab bar follow this root screen's scroll: the bar stays in view while
 * [isScrolledDown] is false and slides away once it turns true. Call it once per root screen,
 * with the state of the list that scrolls the whole page. Does nothing outside the top tab bar
 * layout.
 */
@Composable
internal fun ReportTopTabBarScroll(isScrolledDown: () -> Boolean) {
    val report = LocalTopTabBarScrollReporter.current ?: return
    val readScrolledDown by rememberUpdatedState(isScrolledDown)
    LaunchedEffect(report) {
        snapshotFlow { readScrolledDown() }.distinctUntilChanged().collect { report(it) }
    }
    // A list that leaves composition must not leave its page marked scrolled, bar hidden.
    DisposableEffect(report) {
        onDispose { report(false) }
    }
}

/** Pixels the page is scrolled from its top while its first item is still in view, else null. */
internal fun LazyListState.scrollFromTop(): Int? = if (firstVisibleItemIndex == 0) firstVisibleItemScrollOffset else null

/** Pixels the page is scrolled from its top while its first item is still in view, else null. */
internal fun LazyGridState.scrollFromTop(): Int? = if (firstVisibleItemIndex == 0) firstVisibleItemScrollOffset else null

internal fun LazyListState.isScrolledDown(): Boolean = scrollFromTop() != 0

internal fun LazyGridState.isScrolledDown(): Boolean = scrollFromTop() != 0
