package com.nuvio.tv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.nuvio.tv.ui.theme.NuvioTheme

private const val SPIN_MILLIS = 900
private const val ARC_SWEEP_DEGREES = 100f
private const val STROKE_FRACTION = 0.09f
private const val TRACK_ALPHA = 0.18f

/**
 * The box build's loading indicator: a thin white arc spinning over a faint ring, the plain
 * tvOS spinner. One rotation is read in the draw phase, so it recomposes nothing per frame.
 */
@Composable
internal fun ExpressiveLoadingIndicator(modifier: Modifier, color: Color) {
    val arcColor = if (color == Color.Unspecified) Color.White else color
    val rotation = rememberInfiniteTransition(label = "boxLoading").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(SPIN_MILLIS, easing = LinearEasing)),
        label = "spin"
    )

    // The default size only applies when the caller leaves the size open; a caller's size or
    // fill wins, so the splash mark and small inline spinners draw at the size they lay out.
    Spacer(
        modifier = modifier
            .size(NuvioTheme.spacing.xxxl)
            .drawWithCache {
                val strokeWidth = size.minDimension * STROKE_FRACTION
                val inset = strokeWidth / 2f
                val arcTopLeft = Offset(inset, inset)
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                val track = arcColor.copy(alpha = arcColor.alpha * TRACK_ALPHA)
                onDrawBehind {
                    drawArc(
                        color = track,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = stroke
                    )
                    drawArc(
                        color = arcColor,
                        startAngle = rotation.value - 90f,
                        sweepAngle = ARC_SWEEP_DEGREES,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = stroke
                    )
                }
            }
    )
}
