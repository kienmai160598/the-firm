package com.nuvio.tv.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.nuvio.tv.ui.theme.ExpressiveMotion

/**
 * The top bar's focus pill gliding to [bounds] on the standard spring. Its position, size and
 * fade are read only while drawing, so a glide redraws the pill without recomposing the bar.
 */
@Composable
internal fun SettingsGlidePill(bounds: Rect, visible: Boolean, color: Color, modifier: Modifier) {
    val glideSpec = ExpressiveMotion.standardDefaultSpatial<Float>()
    val left = animateFloatAsState(bounds.left, glideSpec, label = "pillLeft")
    val top = animateFloatAsState(bounds.top, glideSpec, label = "pillTop")
    val width = animateFloatAsState(bounds.width, glideSpec, label = "pillWidth")
    val height = animateFloatAsState(bounds.height, glideSpec, label = "pillHeight")
    val alpha = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = ExpressiveMotion.defaultEffects(),
        label = "pillAlpha"
    )

    Box(
        modifier = modifier.drawBehind {
            if (alpha.value <= 0f) return@drawBehind
            drawRoundRect(
                color = color,
                topLeft = Offset(left.value, top.value),
                size = Size(width.value, height.value),
                cornerRadius = CornerRadius(height.value / 2),
                alpha = alpha.value
            )
        }
    )
}
