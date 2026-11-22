package com.nuvio.tv.ui.screens.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import com.nuvio.tv.ui.theme.ExpressiveMotion
import com.nuvio.tv.ui.theme.isExpressiveBuild

// Settings transitions move on the M3 Expressive springs on the box; other builds keep the
// upstream tween of the same duration.

/** A pane or panel sliding in or out. */
internal fun <T> settingsDetailSlide(upstreamMillis: Int): FiniteAnimationSpec<T> =
    if (isExpressiveBuild) {
        ExpressiveMotion.standardDefaultSpatial()
    } else {
        tween(upstreamMillis, easing = FastOutSlowInEasing)
    }

/** A fade, crossfade or colour change. */
internal fun <T> settingsDetailFade(upstreamMillis: Int): FiniteAnimationSpec<T> =
    if (isExpressiveBuild) ExpressiveMotion.defaultEffects() else tween(upstreamMillis)
