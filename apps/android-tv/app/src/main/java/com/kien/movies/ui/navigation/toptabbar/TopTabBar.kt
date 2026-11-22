package com.kien.movies.ui.navigation.toptabbar

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.kien.movies.DrawerItem
import com.kien.movies.ui.theme.ExpressiveColors

private val TopTabBarTop = 27.dp
internal val TopTabBarHeight = 56.dp
internal val TopTabBarSlot = TopTabBarTop + TopTabBarHeight + 12.dp
internal val TopTabHeight = 48.dp
private val TopTabBarSideMargin = 48.dp

/**
 * The bar floats over the screen and slides with [readProgress]: in view at 1, above it at 0.
 * Focus enters it by moving up, by request, or from anywhere while the content holds no focus,
 * and leaves it only through [TopTabBarState.enterContentFromBar], so a screen that grabs focus
 * while a tab is focused cannot pull focus out of the bar.
 */
@Composable
internal fun TopTabBar(
    items: List<DrawerItem>,
    selectedRoute: String?,
    tabRequesters: Map<String, FocusRequester>,
    profile: TopTabProfile?,
    readProgress: () -> Float,
    state: TopTabBarState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val progress = readProgress()
                translationY = (progress - 1f) * TopTabBarSlot.toPx()
                alpha = progress.coerceIn(0f, 1f)
            }
            .padding(top = TopTabBarTop, start = TopTabBarSideMargin, end = TopTabBarSideMargin)
            .onFocusChanged { state.onBarFocusChanged(it.hasFocus) }
            .focusProperties {
                onEnter = {
                    when {
                        requestedFocusDirection == FocusDirection.Enter -> Unit
                        requestedFocusDirection == FocusDirection.Up -> state.focusSelectedTab()
                        state.contentHasFocus -> cancelFocusChange()
                        else -> state.focusSelectedTab()
                    }
                }
                onExit = { if (!state.leavingBar) cancelFocusChange() }
            }
            .focusGroup()
            .onPreviewKeyEvent { event ->
                val down = event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown
                if (down) state.enterContentFromBar()
                down
            }
    ) {
        TopTabRow(
            items = items,
            selectedRoute = selectedRoute,
            tabRequesters = tabRequesters,
            onTabFocused = state::openTab,
            onTabClicked = state::enterContentFromBar,
            modifier = Modifier.align(Alignment.Center)
        )
        profile?.let {
            TopTabProfileButton(profile = it, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun TopTabRow(
    items: List<DrawerItem>,
    selectedRoute: String?,
    tabRequesters: Map<String, FocusRequester>,
    onTabFocused: (String) -> Unit,
    onTabClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spans = remember { mutableStateMapOf<String, TopTabSpan>() }
    var focusedRoute by remember { mutableStateOf<String?>(null) }
    var pressedRoute by remember { mutableStateOf<String?>(null) }
    val indicatorRoute = focusedRoute ?: selectedRoute
    val tabFocused = focusedRoute != null

    Box(
        modifier = modifier
            .height(TopTabBarHeight)
            .background(ExpressiveColors.barScrim, CircleShape)
            .padding(4.dp)
    ) {
        TopTabIndicator(
            target = indicatorRoute?.let(spans::get),
            barFocused = tabFocused,
            pressed = tabFocused && pressedRoute == focusedRoute,
            modifier = Modifier.matchParentSize()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                TopTab(
                    label = item.label,
                    selected = item.route == selectedRoute,
                    underIndicator = item.route == indicatorRoute,
                    barFocused = tabFocused,
                    onFocusChanged = { isFocused ->
                        if (isFocused && focusedRoute != item.route) {
                            focusedRoute = item.route
                            onTabFocused(item.route)
                        } else if (!isFocused && focusedRoute == item.route) {
                            focusedRoute = null
                        }
                    },
                    onPressChanged = { pressed ->
                        if (pressed) {
                            pressedRoute = item.route
                        } else if (pressedRoute == item.route) {
                            pressedRoute = null
                        }
                    },
                    onPlaced = { span -> if (spans[item.route] != span) spans[item.route] = span },
                    onClick = onTabClicked,
                    modifier = Modifier.focusRequester(tabRequesters.getValue(item.route))
                )
            }
        }
    }
}
