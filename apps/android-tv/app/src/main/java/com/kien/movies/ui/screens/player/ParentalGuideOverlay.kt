@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.accentBrush
import kotlinx.coroutines.delay

private val ROW_HEIGHT = PlayerTenFoot.atLeast(18.dp, 22.dp)
private val OVERLAY_TEXT_SIZE = PlayerTenFoot.atLeast(11.sp, PlayerTenFoot.metaText)
private val ROW_GAP = MoviesTheme.spacing.xxs

/**
 * Parental guide overlay showing content warnings with animated vertical line + staggered items.
 *
 * Triggered by ViewModel when video first starts playing.
 * Completely independent of player controls.
 * Auto-hides after 5 seconds with reverse animation.
 */
@Composable
fun ParentalGuideOverlay(
    warnings: List<ParentalWarning>,
    isVisible: Boolean,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (warnings.isEmpty()) return

    val count = warnings.size
    val totalLineHeight = (ROW_HEIGHT.value * count) + (ROW_GAP.value * (count - 1))
    val accentBrush = MoviesTheme.palette.accentBrush()

    val containerAlpha = remember { Animatable(0f) }
    val lineHeightFraction = remember { Animatable(0f) }
    val itemAlphas = remember(count) { List(count) { Animatable(0f) } }
    var animating by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible && !animating) {
            animating = true

            // FADE IN sequence
            // Container fade in
            containerAlpha.animateTo(1f, tween(300))

            // Line grows top to bottom
            lineHeightFraction.animateTo(1f, tween(400, easing = FastOutSlowInEasing))

            // Items fade in one by one (staggered after line)
            for (i in 0 until count) {
                delay(80)
                itemAlphas[i].animateTo(1f, tween(200))
            }

            // Hold visible for 5 seconds
            delay(5000)

            // FADE OUT sequence: items reverse order (bottom to top)
            for (i in (count - 1) downTo 0) {
                delay(60)
                itemAlphas[i].animateTo(0f, tween(150))
            }

            // Line shrinks
            delay(100)
            lineHeightFraction.animateTo(0f, tween(300, easing = FastOutSlowInEasing))

            // Container fades out
            delay(200)
            containerAlpha.animateTo(0f, tween(200))

            animating = false
            onAnimationComplete()
        } else if (!isVisible && animating) {
            // Quick hide if something forces it off (e.g. controls shown during animation)
            for (i in (count - 1) downTo 0) {
                itemAlphas[i].snapTo(0f)
            }
            lineHeightFraction.snapTo(0f)
            containerAlpha.snapTo(0f)
            animating = false
            onAnimationComplete()
        }
    }

    val hidden by remember { derivedStateOf { containerAlpha.value <= 0f } }
    if (hidden) return

    Row(
        modifier = modifier
            .graphicsLayer { alpha = containerAlpha.value }
            .padding(
                start = PlayerTenFoot.atLeast(MoviesTheme.spacing.xxl, PlayerTenFoot.sideMargin),
                top = PlayerTenFoot.atLeast(MoviesTheme.spacing.xl, PlayerTenFoot.edgeMargin)
            ),
        verticalAlignment = Alignment.Top
    ) {
        // Animated vertical line
        Box(
            modifier = Modifier
                .width(3.dp)
                .growingHeight(totalLineHeight.dp) { lineHeightFraction.value }
                .clip(RoundedCornerShape(MoviesTheme.spacing.hairline))
                .background(accentBrush)
        )

        // Warning items
        Column(
            modifier = Modifier.padding(start = 10.dp),
            verticalArrangement = Arrangement.spacedBy(ROW_GAP)
        ) {
            warnings.forEachIndexed { index, warning ->
                Row(
                    modifier = Modifier
                        .height(ROW_HEIGHT)
                        .graphicsLayer { alpha = itemAlphas.getOrNull(index)?.value ?: 0f },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = warning.label,
                        fontSize = OVERLAY_TEXT_SIZE,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = " · ",
                        fontSize = OVERLAY_TEXT_SIZE,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Text(
                        text = warning.severity,
                        fontSize = OVERLAY_TEXT_SIZE,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
