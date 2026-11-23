package com.kien.movies.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.rememberDrawerState
import com.kien.movies.addons.AddonsScreen
import com.kien.movies.design.Palette
import com.kien.movies.design.RailSize
import com.kien.movies.details.DetailsScreen
import com.kien.movies.home.HomeScreen
import com.kien.movies.search.SearchScreen
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
 * Focus: before the stack changes, the shell saves which element of the screen had focus into the
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

    ForgetRemovedEntries(stack, saveableState, stores)
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

    val route = top.route
    if (route is AppRoute.Tab) {
        WindowBackground(Palette.homeBackground)
        RootTabs(
            selected = route.tab,
            drawerState = drawerState,
            selectedTabFocus = selectedTabFocus,
            onSelect = { tab ->
                router.selectTab(tab)
                contentFocusTick++
            }
        ) {
            EntryContent(top, saveableState, stores, contentFocus) { TabRootScreen(route.tab, router) }
        }
    } else {
        WindowBackground(Palette.detailsBackground)
        EntryContent(top, saveableState, stores, contentFocus) { PushedScreen(route, router) }
    }
}

@Composable
private fun TabRootScreen(tab: RootTab, router: Router) {
    when (tab) {
        RootTab.Home -> HomeScreen(router)

        RootTab.Search -> SearchScreen(router)

        RootTab.Addons -> AddonsScreen()

        RootTab.Library, RootTab.Settings -> PlaceholderScreen(
            title = tab.title,
            modifier = Modifier.padding(start = RailSize.contentStart)
        )
    }
}

@Composable
private fun PushedScreen(route: AppRoute, router: Router) {
    when (route) {
        is AppRoute.Details -> DetailsScreen(route, router)
        is AppRoute.Streams -> StreamsScreen(route, router)
        is AppRoute.Tab -> error("A tab root is never pushed")
    }
}

/** One entry's screen with its own saved state slot and ViewModel store. */
@Composable
private fun EntryContent(
    entry: RouteEntry,
    saveableState: SaveableStateHolder,
    stores: RouteViewModelStores,
    focus: FocusRequester,
    screen: @Composable () -> Unit
) {
    saveableState.SaveableStateProvider(entry.id) {
        CompositionLocalProvider(LocalViewModelStoreOwner provides stores.owner(entry.id)) {
            Box(modifier = Modifier.fillMaxSize().focusRequester(focus).focusRestorer().focusGroup()) {
                screen()
            }
        }
    }
}

/** Drops the saved state and the ViewModels of entries that left the stack. */
@Composable
private fun ForgetRemovedEntries(stack: List<RouteEntry>, saveableState: SaveableStateHolder, stores: RouteViewModelStores) {
    val known = remember { mutableSetOf<Long>() }
    DisposableEffect(stack) {
        val current = stack.map(RouteEntry::id).toSet()
        val tabIds = RootTab.entries.map(Router::tabEntryId).toSet()
        (known - current - tabIds).forEach(saveableState::removeState)
        known.retainAll(current + tabIds)
        known.addAll(current)
        stores.retainOnly(current)
        onDispose {}
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
