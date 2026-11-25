package com.kien.movies.design

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Durations and curves copied from tvOS (docs/tv-look-spec.md section 5). One place, so a screen
 * never times its own motion.
 */
object Motion {
    const val heroAdvanceMillis = 8_000L
    const val controlsHideMillis = 5_000L

    /**
     * Push and pop cross fade, timed on a simulator recording of the tvOS app: Settings to About
     * about 300 ms, Back about 200 ms, ease in and out.
     */
    const val pushMillis = 300
    const val popMillis = 220

    /**
     * A focus move: the lift of the focused control and the scroll that brings it into view run
     * together on this curve, as the tvOS focus engine moves both at once. Critically damped, no
     * overshoot; a 1.0 to 1.1 lift settles in about 500 ms, close to the 530 to 560 ms of changing
     * frames measured after a focus move in the simulator. tv-material's own focus scale (300 ms
     * tween, 500 ms back) and the platform scroll (a stiff spring, under 200 ms) were both faster.
     */
    val focusSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
}
