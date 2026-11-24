package com.kien.movies.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.kien.movies.design.Motion

/**
 * Push and pop as the tvOS app does them (recorded in the simulator): the two screens cross fade
 * in place, with no slide, so what both screens share (the Settings art) seems to stay put. Every
 * tab root shares one slot, so a tab change does not fade.
 *
 * Alpha is set in the draw phase and modulates each draw instead of going through an offscreen
 * layer, which on the box would cost a second full screen fill per screen and frame.
 */
@Composable
fun Transition<RouteEntry>.RouteContent(content: @Composable (entry: RouteEntry) -> Unit) {
    val route = this
    AnimatedContent(
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished using null },
        contentKey = ::routeSlotKey
    ) { entry ->
        Box(modifier = Modifier.fillMaxSize().then(routeFade(route))) { content(entry) }
    }
}

/** True once the last push or pop has finished fading. */
val Transition<RouteEntry>.isSettled: Boolean get() = !isRunning && currentState == targetState

private object TabSlot

private fun routeSlotKey(entry: RouteEntry): Any = if (entry.route is AppRoute.Tab) TabSlot else entry.id

/** A pushed entry has a larger id than the one under it; tab roots have negative ids. */
private fun Transition<RouteEntry>.isPush(): Boolean = targetState.id > currentState.id

@Composable
private fun AnimatedVisibilityScope.routeFade(route: Transition<RouteEntry>): Modifier {
    val alpha = transition.animateFloat(
        transitionSpec = {
            val millis = if (route.isPush()) Motion.pushMillis else Motion.popMillis
            tween(millis, easing = FastOutSlowInEasing)
        },
        label = "routeFade"
    ) { if (it == EnterExitState.Visible) 1f else 0f }
    return Modifier.graphicsLayer {
        this.alpha = alpha.value
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
}
