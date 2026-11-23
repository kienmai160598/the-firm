@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import com.kien.movies.ui.theme.ExpressiveMotion
import com.kien.movies.ui.theme.MoviesTheme

private val IndicatorIconSize = MoviesTheme.spacing.xxl
private val IndicatorEdgeInset = MoviesTheme.spacing.xxs

/**
 * The strip a vertical chevron covers at a list's top or bottom edge. A list keeps its rows out
 * of it, so the chevron never sits on top of a row.
 */
internal val SettingsVerticalIndicatorBand: Dp = IndicatorIconSize + IndicatorEdgeInset

@Composable
internal fun BoxScope.SettingsVerticalScrollIndicators(
    state: ScrollableState,
    modifier: Modifier = Modifier,
    suppressBackward: Boolean = false,
    suppressForward: Boolean = false,
    forceBackward: Boolean = false,
    forceForward: Boolean = false
) {
    AnimatedVisibility(
        visible = (state.canScrollBackward || forceBackward) && !suppressBackward,
        modifier = modifier
            .align(Alignment.TopCenter)
            .padding(top = IndicatorEdgeInset),
        enter = fadeIn(animationSpec = ExpressiveMotion.defaultEffects()),
        exit = fadeOut(animationSpec = ExpressiveMotion.defaultEffects())
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            tint = MoviesTheme.colors.TextSecondary.copy(alpha = 0.55f),
            modifier = Modifier.size(IndicatorIconSize)
        )
    }
    AnimatedVisibility(
        visible = (state.canScrollForward || forceForward) && !suppressForward,
        modifier = modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = IndicatorEdgeInset),
        enter = fadeIn(animationSpec = ExpressiveMotion.defaultEffects()),
        exit = fadeOut(animationSpec = ExpressiveMotion.defaultEffects())
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MoviesTheme.colors.TextSecondary.copy(alpha = 0.55f),
            modifier = Modifier.size(IndicatorIconSize)
        )
    }
}

@Composable
internal fun BoxScope.SettingsHorizontalScrollIndicators(
    state: ScrollableState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.canScrollBackward,
        modifier = modifier
            .align(Alignment.CenterStart)
            .padding(start = IndicatorEdgeInset),
        enter = fadeIn(animationSpec = ExpressiveMotion.defaultEffects()),
        exit = fadeOut(animationSpec = ExpressiveMotion.defaultEffects())
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = MoviesTheme.colors.TextSecondary.copy(alpha = 0.55f),
            modifier = Modifier.size(IndicatorIconSize)
        )
    }
    AnimatedVisibility(
        visible = state.canScrollForward,
        modifier = modifier
            .align(Alignment.CenterEnd)
            .padding(end = IndicatorEdgeInset),
        enter = fadeIn(animationSpec = ExpressiveMotion.defaultEffects()),
        exit = fadeOut(animationSpec = ExpressiveMotion.defaultEffects())
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MoviesTheme.colors.TextSecondary.copy(alpha = 0.55f),
            modifier = Modifier.size(IndicatorIconSize)
        )
    }
}
