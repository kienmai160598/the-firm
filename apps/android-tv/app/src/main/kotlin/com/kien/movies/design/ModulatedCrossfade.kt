package com.kien.movies.design

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Compose `Crossfade` with the alpha applied to each draw call ([CompositingStrategy.ModulateAlpha])
 * instead of through an offscreen layer, so a crossfade of text over art costs the box GPU no
 * extra full size buffer. The old and the new [content] overlap while they fade (ease in and out,
 * as SwiftUI `.easeInOut`), so use it for content that does not overlap itself.
 */
@Composable
fun <T> ModulatedCrossfade(targetState: T, durationMillis: Int, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    val transition = updateTransition(targetState, label = "ModulatedCrossfade")
    val visible = remember { mutableStateListOf(transition.currentState) }
    if (transition.currentState == transition.targetState) {
        if (visible.size != 1 || visible[0] != transition.targetState) {
            visible.removeAll { state -> state != transition.targetState }
            if (visible.isEmpty()) visible.add(transition.targetState)
        }
    } else if (transition.targetState !in visible) {
        visible.add(transition.targetState)
    }
    Box(modifier = modifier) {
        visible.forEach { state ->
            key(state) {
                val alpha by transition.animateFloat(
                    transitionSpec = { tween(durationMillis, easing = EaseInOut) },
                    label = "alpha"
                ) { shown -> if (shown == state) 1f else 0f }
                Box(
                    modifier = Modifier.graphicsLayer {
                        this.alpha = alpha
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
                ) {
                    content(state)
                }
            }
        }
    }
}
