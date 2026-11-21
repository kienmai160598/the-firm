package com.nuvio.tv.ui.navigation.toptabbar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.nuvio.tv.LocalContentFocusRequester
import com.nuvio.tv.ui.navigation.Screen

/** Runs Back on a root screen; null when the navigation layout handles Back itself. */
internal val LocalRootBack = staticCompositionLocalOf<(() -> Unit)?> { null }

/** Focuses the root screen in view, for a screen with no [LocalContentFocusRequester] target. */
internal val LocalTopTabBarScreenFocus = staticCompositionLocalOf<FocusRequester?> { null }

/** Root routes whose screen keeps the bar in view, like a tvOS split view. */
internal val topTabBarPinningRoutes = setOf(Screen.Settings.route)

/**
 * Wraps a root destination of the top tab bar layout: gives it [LocalTopTabBarInset] and a
 * scroll reporter, pushes pinning routes below the bar, sends Back to the bar, and keeps focus
 * and Back away from it while the NavHost transition is taking it off screen. Shows [content]
 * as it is in other layouts and when [withInset] is false.
 */
@Composable
internal fun AnimatedVisibilityScope.TopTabBarRootFrame(
    route: String,
    withInset: Boolean,
    content: @Composable () -> Unit
) {
    val slot = if (withInset) LocalTopTabBarSlot.current else 0.dp
    val rootBack = LocalRootBack.current
    val screenFocus = LocalTopTabBarScreenFocus.current
    if (slot == 0.dp || rootBack == null || screenFocus == null) {
        content()
        return
    }

    val isLeaving = transition.targetState != EnterExitState.Visible
    val pinnedTop = if (route in topTabBarPinningRoutes) slot else 0.dp
    BackHandler(enabled = !isLeaving, onBack = rootBack)
    ProvideTopTabBarScrollReporter(route) {
        CompositionLocalProvider(LocalTopTabBarInset provides slot - pinnedTop) {
            Box(modifier = Modifier.padding(top = pinnedTop).screenFocus(isLeaving, screenFocus)) {
                content()
            }
        }
    }
}

// During the crossfade the leaving screen still has focusable items under the bar; focus put
// there is lost when the screen is disposed, so only the screen in view takes [screenFocus].
// The blocking group exists only while leaving: a focus search rates a group by its own bounds,
// and a full-screen group is never below the bar.
private fun Modifier.screenFocus(leaving: Boolean, screenFocus: FocusRequester): Modifier =
    if (leaving) {
        focusProperties { onEnter = { cancelFocusChange() } }.focusGroup()
    } else {
        focusRequester(screenFocus)
    }
