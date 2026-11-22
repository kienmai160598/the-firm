@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.SelectableChipBorder
import androidx.tv.material3.SelectableChipColors
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.ExpressiveMotion
import com.kien.movies.ui.theme.MoviesTheme

private const val ERROR_CONTAINER_ALPHA = 0.14f
private const val FOCUSED_ERROR_CONTAINER_ALPHA = 0.26f
private const val ERROR_PULSE_ALPHA = 0.9f
private const val ERROR_SHAKE_VELOCITY = 160f
private val CheckIconSize = 18.dp

// A loose spring started with a push swings past rest a few times and settles, so one animation
// gives the shake that a sequence of tweens used to.
private val errorShakeSpring =
    spring<Float>(dampingRatio = 0.25f, stiffness = Spring.StiffnessMedium)

@Composable
internal fun filterChipContentColor(focused: Boolean, selected: Boolean, isError: Boolean): Color {
    val tone = ExpressiveFocus.contentColor(
        focused = focused,
        selected = selected,
        resting = MoviesTheme.colors.TextPrimary
    )
    return if (isError) MoviesTheme.colors.Error else tone
}

@Composable
internal fun filterChipColors(
    focused: Boolean,
    selected: Boolean,
    isError: Boolean,
    content: Color
): SelectableChipColors {
    val tone = ExpressiveFocus.containerColor(
        focused = focused,
        resting = chipRestingContainer(selected)
    )
    val container = if (isError) errorContainer(focused) else tone
    return FilterChipDefaults.colors(
        containerColor = container,
        contentColor = content,
        focusedContainerColor = container,
        focusedContentColor = content,
        pressedContainerColor = container,
        pressedContentColor = content,
        selectedContainerColor = container,
        selectedContentColor = content,
        focusedSelectedContainerColor = container,
        focusedSelectedContentColor = content,
        pressedSelectedContainerColor = container,
        pressedSelectedContentColor = content
    )
}

/** Frosted at rest, brighter while selected. */
@Composable
private fun chipRestingContainer(selected: Boolean): Color = when {
    selected -> ExpressiveColors.frostedStrong
    else -> ExpressiveColors.frosted
}

@Composable
private fun errorContainer(focused: Boolean): Color = MoviesTheme.colors.Error.copy(
    alpha = if (focused) FOCUSED_ERROR_CONTAINER_ALPHA else ERROR_CONTAINER_ALPHA
)

/** Every state set, because tv-material's defaults draw outlines the M3E chip does not have. */
@Composable
internal fun borderlessFilterChip(): SelectableChipBorder = FilterChipDefaults.border(
    border = Border.None,
    focusedBorder = Border.None,
    pressedBorder = Border.None,
    selectedBorder = Border.None,
    disabledBorder = Border.None,
    focusedSelectedBorder = Border.None,
    focusedDisabledBorder = Border.None,
    pressedSelectedBorder = Border.None,
    selectedDisabledBorder = Border.None,
    focusedSelectedDisabledBorder = Border.None
)

internal fun selectedCheckIcon(selected: Boolean, tint: Color): (@Composable () -> Unit)? =
    if (selected) {
        {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(CheckIconSize),
                tint = tint
            )
        }
    } else {
        null
    }

internal suspend fun playErrorNudge(
    alpha: Animatable<Float, AnimationVector1D>,
    offsetPx: Animatable<Float, AnimationVector1D>
) {
    alpha.animateTo(ERROR_PULSE_ALPHA, ExpressiveMotion.defaultEffects())
    alpha.animateTo(1f, ExpressiveMotion.defaultEffects())
    offsetPx.animateTo(0f, errorShakeSpring, initialVelocity = ERROR_SHAKE_VELOCITY)
}
