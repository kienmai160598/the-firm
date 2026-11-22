package com.kien.movies.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.ui.theme.ExpressiveMotion
import com.kien.movies.ui.theme.MoviesTheme
import kotlinx.coroutines.launch

// Sized for a 960 x 540 dp TV screen: the lockup spans about half the width and a third of
// the height, so it reads from the sofa and still leaves the screen calm.
private val MarkSize = 200.dp
private val PlayGlyphSize = 72.dp
private val LockupGap = 40.dp
private val WordmarkSize = 88.sp
private val WordmarkEntryShift = (-32).dp
private const val MARK_ENTRY_SCALE = 0.9f
private const val GLOW_CENTER_X = 0.3f
private const val GLOW_CENTER_Y = 0.4f
private const val GLOW_RADIUS_FRACTION = 0.75f
private const val GLOW_ALPHA = 0.22f

/** The brand surface of the box build's splash, the same glow the launcher banner carries. */
@Composable
internal fun MoviesSplashBackground(modifier: Modifier = Modifier) {
    val surface = MoviesTheme.colors.Background
    val glow = MoviesTheme.colors.Secondary.copy(alpha = GLOW_ALPHA)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface)
            .background(brandGlow(glow))
    )
}

private fun brandGlow(glow: Color): Brush =
    object : ShaderBrush() {
        override fun createShader(size: Size): Shader =
            RadialGradientShader(
                center = Offset(size.width * GLOW_CENTER_X, size.height * GLOW_CENTER_Y),
                radius = size.width * GLOW_RADIUS_FRACTION,
                colors = listOf(glow, glow.copy(alpha = 0f))
            )
    }

/**
 * The box build's startup content: a horizontal lockup made for a 16:9 screen. The mark is the
 * expressive loading indicator itself, morphing behind a still play glyph, and the name slides
 * in beside it once.
 */
@Composable
internal fun MoviesSplashContent() {
    val markScale = remember { Animatable(MARK_ENTRY_SCALE) }
    val wordmarkProgress = remember { Animatable(0f) }
    val entryShiftPx = with(LocalDensity.current) { WordmarkEntryShift.toPx() }
    LaunchedEffect(Unit) {
        launch { markScale.animateTo(1f, ExpressiveMotion.expressiveFastSpatial()) }
        wordmarkProgress.animateTo(1f, ExpressiveMotion.standardDefaultSpatial())
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(LockupGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MoviesMark(
            modifier = Modifier.graphicsLayer {
                scaleX = markScale.value
                scaleY = markScale.value
            }
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = WordmarkSize,
                lineHeight = WordmarkSize,
                fontWeight = FontWeight.Medium
            ),
            color = MoviesTheme.colors.TextPrimary,
            maxLines = 1,
            modifier = Modifier.graphicsLayer {
                val progress = wordmarkProgress.value
                alpha = progress.coerceIn(0f, 1f)
                translationX = entryShiftPx * (1f - progress)
            }
        )
    }
}

@Composable
private fun MoviesMark(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(MarkSize), contentAlignment = Alignment.Center) {
        LoadingIndicator(
            modifier = Modifier.fillMaxSize(),
            color = MoviesTheme.colors.Secondary
        )
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = MoviesTheme.colors.OnSecondary,
            modifier = Modifier.size(PlayGlyphSize)
        )
    }
}
