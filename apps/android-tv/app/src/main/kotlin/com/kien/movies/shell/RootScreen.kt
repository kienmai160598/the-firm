package com.kien.movies.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.rememberDrawerState
import com.kien.movies.design.Palette
import com.kien.movies.design.TvColors
import com.kien.movies.details.DetailsScreen
import com.kien.movies.home.HomeScreen
import com.kien.movies.search.SearchScreen
import com.kien.movies.settings.SettingsPageScreen
import com.kien.movies.settings.SettingsScreen
import com.kien.movies.streams.StreamsScreen

/**
 * The shell (tvOS RootTabView.swift): the sidebar with the tab roots, and the pushed routes full
 * screen over it, as tvOS pushes its NavigationStack over the TabView.
 *
 * Back, in order (docs/android-tv-rebuild.md, section 4, focus rules):
 * 1. A screen's own BackHandler (player, dialog, panel) wins while it is enabled.
 * 2. Deeper than a tab root: pop.
 * 3. Tab root, focus in the content: move focus to the sidebar, on the selected tab.
 * 4. Sidebar focused on another tab: select Home, focus stays in the sidebar.
 * 5. Sidebar focused on Home: [onExit].
 *
 * Push and pop move as in [RouteContent]. Focus: before the stack changes, the shell saves which element of the screen had focus into the
 * entry's saved state; after every change of the top entry the screen's root group gets focus,
 * which lands on that saved element when the entry comes back (AC N4), else on the screen's first
 * focusable child. A screen that wants another first focus sets `focusProperties { onEnter }` or
 * its own `focusRestorer` on its root.
 */
@Composable
fun RootScreen(router: Router, stores: RouteViewModelStores, onExit: () -> Unit) {
    val stack by router.stack.collectAsStateWithLifecycle()
    val top = stack.last()
    val saveableState = rememberSaveableStateHolder()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val selectedTabFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }
    var contentFocusTick by remember { mutableIntStateOf(0) }
    var drawerFocusTick by remember { mutableIntStateOf(0) }

    val transition = updateTransition(top, label = "route")
    ForgetRemovedEntries(stack, saveableState, stores, settled = transition.isSettled)
    DisposableEffect(router) {
        router.beforeChange = { contentFocus.saveFocusedChild() }
        onDispose { router.beforeChange = null }
    }

    BackHandler {
        when {
            !router.isAtTabRoot -> router.back()

            drawerState.currentValue == DrawerValue.Closed -> {
                contentFocus.saveFocusedChild()
                drawerFocusTick++
            }

            router.back() == BackResult.Exit -> onExit()

            else -> drawerFocusTick++
        }
    }

    LaunchedEffect(top.id) {
        if (drawerState.currentValue == DrawerValue.Closed) contentFocusTick++
    }
    FocusAfterFrame(contentFocus, contentFocusTick)
    FocusAfterFrame(selectedTabFocus, drawerFocusTick)

    WindowBackground(windowBackground(top.route))
    transition.RouteContent { entry ->
        val route = entry.route
        val isTarget = entry.id == transition.targetState.id
        if (route is AppRoute.Tab) {
            RootTabs(
                selected = route.tab,
                drawerState = drawerState,
                selectedTabFocus = selectedTabFocus,
                contentFocus = contentFocus,
                onSelect = { tab ->
                    router.selectTab(tab)
                    contentFocusTick++
                }
            ) {
                EntryContent(entry, saveableState, stores, contentFocus, isTarget) { TabRootScreen(route.tab, router) }
            }
        } else {
            EntryContent(entry, saveableState, stores, contentFocus, isTarget) { PushedScreen(route, router) }
        }
    }
}

/**
 * Window colour (spec section 2): Home's rows, the grey slate of Search, Library and Settings with
 * its pages, and the Details colour behind Details and Streams.
 */
private fun windowBackground(route: AppRoute): Color = when {
    route is AppRoute.Tab && route.tab == RootTab.Home -> TvColors.homeRows
    route is AppRoute.Tab || route is AppRoute.Settings -> TvColors.searchLibrary
    else -> Palette.detailsBackground
}

@Composable
private fun TabRootScreen(tab: RootTab, router: Router) {
    when (tab) {
        RootTab.Home -> HomeScreen(router)
        RootTab.Search -> SearchScreen(router)
        RootTab.Settings -> SettingsScreen(router)
        RootTab.Library -> PlaceholderScreen(title = tab.title)
    }
}

@Composable
private fun PushedScreen(route: AppRoute, router: Router) {
    when (route) {
        is AppRoute.Details -> DetailsScreen(route, router)
        is AppRoute.Streams -> StreamsScreen(route, router)
        is AppRoute.Settings -> SettingsPageScreen(route.page)
        is AppRoute.Tab -> error("A tab root is never pushed")
    }
}

/** Requests focus one frame after [tick] changes, once the new screen is attached and laid out. */
@Composable
private fun FocusAfterFrame(requester: FocusRequester, tick: Int) {
    LaunchedEffect(tick) {
        if (tick == 0) return@LaunchedEffect
        withFrameNanos {}
        requester.requestFocus()
    }
}
