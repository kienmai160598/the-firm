package com.kien.movies.ui.navigation.toptabbar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.navigation.NavHostController
import com.kien.movies.DrawerItem
import com.kien.movies.LocalContentFocusRequester
import com.kien.movies.LocalSidebarExpanded
import com.kien.movies.ui.navigation.NuvioNavHost
import com.kien.movies.ui.navigation.Screen
import com.kien.movies.ui.theme.ExpressiveMotion
import com.kien.movies.ui.util.rememberDrawerItemFocusRequesters

@Composable
internal fun TopTabBarScaffold(
    longPressBackHeld: MutableState<Boolean>,
    navController: NavHostController,
    startDestination: String,
    currentRoute: String?,
    rootRoutes: Set<String>,
    drawerItems: List<DrawerItem>,
    selectedDrawerRoute: String?,
    activeProfileName: String,
    activeProfileColorHex: String,
    activeProfileAvatarImageUrl: String?,
    showProfileSelector: Boolean,
    onSwitchProfile: () -> Unit,
    onNavigate: (String) -> Unit,
    onExitApp: () -> Unit
) {
    val onRootRoute = currentRoute in rootRoutes
    val tabRequesters = rememberDrawerItemFocusRequesters(drawerItems)
    val state = rememberTopTabBarState(
        navController = navController,
        longPressBackHeld = longPressBackHeld,
        selectedTabRequester = tabRequesters[selectedDrawerRoute]
            ?: tabRequesters.values.firstOrNull(),
        onNavigate = onNavigate,
        onExitApp = onExitApp
    )
    val scrollRegistry = remember { TopTabBarScrollRegistry() }
    val readBarProgress = animateBarProgress(
        shown = onRootRoute && TopTabBarRules.barVisible(
            focus = state.focus,
            routePinsBar = currentRoute in topTabBarPinningRoutes,
            scrolledDown = scrollRegistry.isScrolledDown(currentRoute)
        )
    )
    val rootBack = remember(state) { state::back }
    LaunchedEffect(currentRoute) { longPressBackHeld.value = false }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onLongPressBack(
                enabled = { onRootRoute && !state.barHasFocus },
                held = longPressBackHeld,
                onLongPress = { state.perform(TopTabBarStep.FocusOn(TopTabBarFocus.Bar)) }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { state.onContentFocusChanged(it.hasFocus) }
                .onKeyEvent { event ->
                    if (!onRootRoute || !event.isUpPress()) return@onKeyEvent false
                    state.moveUpFromContent()
                    true
                }
        ) {
            CompositionLocalProvider(
                // Home holds back row expansion and trailers while a tab has focus.
                LocalSidebarExpanded provides state.barHasFocus,
                LocalTopTabBarSlot provides TopTabBarSlot,
                LocalRootBack provides rootBack,
                LocalTopTabBarScrollRegistry provides scrollRegistry,
                LocalContentFocusRequester provides state.contentFocusRequester,
                LocalTopTabBarScreenFocus provides state.screenFocusRequester
            ) {
                NuvioNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    hideBuiltInHeaders = false,
                    discoverIsRoot = Screen.Discover.route in rootRoutes
                )
            }
        }

        if (onRootRoute) {
            TopTabBar(
                items = drawerItems,
                selectedRoute = selectedDrawerRoute,
                tabRequesters = tabRequesters,
                profile = topTabProfile(
                    show = showProfileSelector,
                    name = activeProfileName,
                    colorHex = activeProfileColorHex,
                    avatarImageUrl = activeProfileAvatarImageUrl,
                    onClick = onSwitchProfile
                ),
                readProgress = readBarProgress,
                state = state
            )
        }
    }
}

@Composable
private fun animateBarProgress(shown: Boolean): () -> Float {
    val progress = animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = ExpressiveMotion.standardDefaultSpatial(),
        label = "topTabBarProgress"
    )
    return remember(progress) { { progress.value } }
}

private fun topTabProfile(
    show: Boolean,
    name: String,
    colorHex: String,
    avatarImageUrl: String?,
    onClick: () -> Unit
): TopTabProfile? = if (show) TopTabProfile(name, colorHex, avatarImageUrl, onClick) else null
