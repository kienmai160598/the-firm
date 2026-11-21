package com.nuvio.tv.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import com.nuvio.tv.domain.model.AppTheme

// Material 3 Expressive motion (MotionScheme.expressive()) as the box's launcher uses it.
internal object ExpressiveMotion {
    /** Fast spatial spring: small parts that move or scale, with a little overshoot. */
    fun <T> expressiveFastSpatial(): SpringSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)

    /** Standard default spatial spring: large surfaces that move without overshoot. */
    fun <T> standardDefaultSpatial(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = 700f)

    /** Default effects spring: colour and opacity changes. */
    fun <T> defaultEffects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)

    /** Expressive cards zoom on focus like the launcher's tiles; other themes keep still cards. */
    fun cardFocusScale(theme: AppTheme): Float =
        if (theme == AppTheme.EXPRESSIVE) EXPRESSIVE_CARD_FOCUS_SCALE else 1f

    private const val EXPRESSIVE_CARD_FOCUS_SCALE = 1.06f
}
