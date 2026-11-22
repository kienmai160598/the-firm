package com.kien.movies.ui.navigation.toptabbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.navigation.NavHostController
import com.kien.movies.navigateToDrawerRoute

/** Where focus is in the top tab bar layout, and the moves between the bar and the content. */
@Stable
internal class TopTabBarState(
    private val navController: NavHostController,
    private val focusManager: FocusManager,
    private val keyboardController: State<SoftwareKeyboardController?>,
    private val longPressBackHeld: MutableState<Boolean>,
    private val selectedTabRequester: State<FocusRequester?>,
    private val onNavigate: State<(String) -> Unit>,
    private val onExitApp: State<() -> Unit>
) {
    val contentFocusRequester = FocusRequester()
    val screenFocusRequester = FocusRequester()

    var barHasFocus by mutableStateOf(false)
        private set

    var leavingBar by mutableStateOf(false)
        private set

    var contentHasFocus by mutableStateOf(false)
        private set

    val focus: TopTabBarFocus
        get() = if (barHasFocus) TopTabBarFocus.Bar else TopTabBarFocus.Content

    fun onBarFocusChanged(hasFocus: Boolean) {
        barHasFocus = hasFocus
        if (!hasFocus) leavingBar = false
    }

    fun onContentFocusChanged(hasFocus: Boolean) {
        contentHasFocus = hasFocus
    }

    fun enterContentFromBar() = perform(TopTabBarRules.step(focus, TopTabBarInput.EnterContent))

    private fun enterContent() {
        leavingBar = true
        // A list that fills the screen under the bar is never below it, so a Down search from the
        // bar misses it; ask the screen first.
        val moved = contentFocusRequester.requestFocus(FocusDirection.Enter) ||
            screenFocusRequester.requestFocus(FocusDirection.Enter) ||
            focusManager.moveFocus(FocusDirection.Down)
        if (!moved) leavingBar = false
    }

    fun perform(step: TopTabBarStep) {
        when (step) {
            is TopTabBarStep.FocusOn -> when (step.focus) {
                TopTabBarFocus.Bar -> focusSelectedTab()
                TopTabBarFocus.Content -> enterContent()
            }
            TopTabBarStep.ExitApp -> if (!longPressBackHeld.value) onExitApp.value()
        }
    }

    fun focusSelectedTab() {
        selectedTabRequester.value?.requestFocus(FocusDirection.Enter)
    }

    fun back() = perform(TopTabBarRules.step(focus, TopTabBarInput.Back))

    fun moveUpFromContent() {
        if (!focusManager.moveFocus(FocusDirection.Up)) {
            perform(TopTabBarRules.step(focus, TopTabBarInput.UpFromTop))
        }
    }

    // Reads the NavController, not the route of the last composition: two tab focus changes in
    // one frame must compare against the route the first one already navigated to.
    fun openTab(route: String) {
        val shownRoute = navController.currentDestination?.route
        if (route == shownRoute) return
        keyboardController.value?.hide()
        onNavigate.value(route)
        navigateToDrawerRoute(navController, shownRoute, route)
    }
}

@Composable
internal fun rememberTopTabBarState(
    navController: NavHostController,
    longPressBackHeld: MutableState<Boolean>,
    selectedTabRequester: FocusRequester?,
    onNavigate: (String) -> Unit,
    onExitApp: () -> Unit
): TopTabBarState {
    val focusManager = LocalFocusManager.current
    val keyboardController = rememberUpdatedState(LocalSoftwareKeyboardController.current)
    val selectedTab = rememberUpdatedState(selectedTabRequester)
    val navigate = rememberUpdatedState(onNavigate)
    val exitApp = rememberUpdatedState(onExitApp)
    return remember(navController, focusManager, longPressBackHeld) {
        TopTabBarState(
            navController = navController,
            focusManager = focusManager,
            keyboardController = keyboardController,
            longPressBackHeld = longPressBackHeld,
            selectedTabRequester = selectedTab,
            onNavigate = navigate,
            onExitApp = exitApp
        )
    }
}
