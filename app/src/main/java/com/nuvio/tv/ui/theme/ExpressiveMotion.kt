package com.nuvio.tv.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import com.nuvio.tv.domain.model.AppTheme

// The box build's motion: M3 Expressive's scheme with its fast spatial spring softened to the
// tvOS focus lift, which settles with almost no overshoot.
internal object ExpressiveMotion {
    /** A focused poster or card lifts this much, as on Apple TV. */
    const val EXPRESSIVE_CARD_FOCUS_SCALE = 1.08f

    /** One sweep of every loading shimmer, so skeletons and placeholders pulse together. */
    const val SHIMMER_SWEEP_MILLIS = 1600

    /** The tvOS focus lift: what a focused card, button or row scales and rises with. */
    fun <T> focusLift(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 400f)

    /** Small parts that move or scale; on this build the same spring as [focusLift]. */
    fun <T> expressiveFastSpatial(): SpringSpec<T> = focusLift()

    /**
     * Expressive default spatial spring: what Material 3's tab indicator slides and resizes with
     * (TabRow uses MotionSchemeKeyTokens.DefaultSpatial).
     */
    fun <T> expressiveDefaultSpatial(): SpringSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 380f)

    /** Standard default spatial spring: large surfaces that move without overshoot. */
    fun <T> standardDefaultSpatial(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = 700f)

    /** Default effects spring: colour and opacity changes. */
    fun <T> defaultEffects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)

    /** Slow effects spring: larger fades, such as a panel or an image appearing. */
    fun <T> slowEffects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 800f)

    /** [defaultEffects] on the expressive build, [upstream] elsewhere: one-shot fades. */
    fun <T> effectsOr(upstream: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
        if (isExpressiveBuild) defaultEffects() else upstream

    /** [slowEffects] on the expressive build, [upstream] elsewhere: larger one-shot fades. */
    fun <T> slowEffectsOr(upstream: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
        if (isExpressiveBuild) slowEffects() else upstream

    /** [expressiveFastSpatial] on the expressive build, [upstream] elsewhere: small movements. */
    fun <T> fastSpatialOr(upstream: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
        if (isExpressiveBuild) expressiveFastSpatial() else upstream

    /** [standardDefaultSpatial] on the expressive build, [upstream] elsewhere: large surfaces. */
    fun <T> defaultSpatialOr(upstream: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
        if (isExpressiveBuild) standardDefaultSpatial() else upstream

    /** Expressive cards zoom on focus like the launcher's tiles; other themes keep still cards. */
    fun cardFocusScale(theme: AppTheme): Float =
        if (theme == AppTheme.EXPRESSIVE) EXPRESSIVE_CARD_FOCUS_SCALE else 1f
}
