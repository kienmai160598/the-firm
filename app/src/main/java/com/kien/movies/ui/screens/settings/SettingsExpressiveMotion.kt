@file:Suppress("UNUSED_PARAMETER")

package com.kien.movies.ui.screens.settings

import androidx.compose.animation.core.FiniteAnimationSpec
import com.kien.movies.ui.theme.ExpressiveMotion

// Settings transitions move on the M3 Expressive springs; the millisecond argument is not read.

/** A pane or panel sliding in or out. */
internal fun <T> settingsDetailSlide(upstreamMillis: Int): FiniteAnimationSpec<T> =
    ExpressiveMotion.standardDefaultSpatial()

/** A fade, crossfade or colour change. */
internal fun <T> settingsDetailFade(upstreamMillis: Int): FiniteAnimationSpec<T> =
    ExpressiveMotion.defaultEffects()
