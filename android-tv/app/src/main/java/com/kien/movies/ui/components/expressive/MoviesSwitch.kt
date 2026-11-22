@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.components.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveMotion

private val TrackWidth = 52.dp
private val TrackHeight = 32.dp
private val TrackOutline = 2.dp
private val ThumbOffSize = 16.dp
private val ThumbOnSize = 24.dp
private val ThumbFocusedOffSize = 20.dp
private val ThumbFocusedOnSize = 28.dp
private val CheckIconSize = 16.dp
private const val DISABLED_ALPHA = 0.38f
private const val OFF_TRACK_ALPHA = 0.18f
private const val OFF_THUMB_ALPHA = 0.64f

/**
 * The M3E switch in the Apple TV palette: a white track with a large thumb when on, a frosted
 * track with a small thumb when off. The thumb slides and grows on the expressive spring, and a
 * step more while its row is [focused]; the row then turns white, so the switch inverts to black.
 * It draws only: the row around it takes focus and clicks.
 */
@Composable
internal fun MoviesSwitch(checked: Boolean, enabled: Boolean, focused: Boolean) {
    val thumbSize = animateDpAsState(
        targetValue = thumbSize(checked, focused),
        animationSpec = ExpressiveMotion.expressiveFastSpatial(),
        label = "switchThumbSize"
    )
    val thumbCenter = animateDpAsState(
        targetValue = if (checked) TrackWidth - TrackHeight / 2 else TrackHeight / 2,
        animationSpec = ExpressiveMotion.expressiveFastSpatial(),
        label = "switchThumbCenter"
    )
    val palette = switchPalette(checked, onWhiteRow = focused)
    val trackColor = animatedSwitchColor(palette.track)
    val outlineColor = animatedSwitchColor(palette.outline)
    val thumbColor = animatedSwitchColor(palette.thumb)

    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .size(TrackWidth, TrackHeight)
            .background(trackColor, CircleShape)
            .border(TrackOutline, outlineColor, CircleShape),
        contentAlignment = Alignment.CenterStart
    ) {
        SwitchThumb(
            size = thumbSize,
            center = thumbCenter,
            color = thumbColor,
            checkTint = trackColor,
            showCheck = checked
        )
    }
}

@Composable
private fun SwitchThumb(
    size: State<Dp>,
    center: State<Dp>,
    color: Color,
    checkTint: Color,
    showCheck: Boolean
) {
    Box(
        modifier = Modifier
            .offset { IntOffset((center.value - size.value / 2).roundToPx(), 0) }
            .size(size.value)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (showCheck) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = checkTint,
                modifier = Modifier.size(CheckIconSize)
            )
        }
    }
}

private data class SwitchPalette(val track: Color, val outline: Color, val thumb: Color)

private fun switchPalette(checked: Boolean, onWhiteRow: Boolean): SwitchPalette {
    val ink = if (onWhiteRow) ExpressiveColors.onFocus else ExpressiveColors.focus
    val paper = if (onWhiteRow) ExpressiveColors.focus else ExpressiveColors.onFocus
    return if (checked) {
        SwitchPalette(track = ink, outline = Color.Transparent, thumb = paper)
    } else {
        SwitchPalette(
            track = ink.copy(alpha = OFF_TRACK_ALPHA),
            outline = Color.Transparent,
            thumb = ink.copy(alpha = OFF_THUMB_ALPHA)
        )
    }
}

private fun thumbSize(checked: Boolean, focused: Boolean): Dp = when {
    checked && focused -> ThumbFocusedOnSize
    checked -> ThumbOnSize
    focused -> ThumbFocusedOffSize
    else -> ThumbOffSize
}

@Composable
private fun animatedSwitchColor(target: Color): Color {
    val color = animateColorAsState(
        targetValue = target,
        animationSpec = ExpressiveMotion.defaultEffects(),
        label = "switchColor"
    )
    return color.value
}
