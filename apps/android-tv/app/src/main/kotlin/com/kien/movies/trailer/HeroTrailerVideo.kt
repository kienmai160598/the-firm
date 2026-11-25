package com.kien.movies.trailer

import android.view.SurfaceView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import com.kien.movies.design.BakedScrim
import com.kien.movies.design.ScrimOverlay
import kotlin.math.roundToInt

/** What a backdrop shows of its trailer: the video once [isShowing], filling it and cropped by [zoom]. */
data class HeroTrailerPicture(val itemId: String, val player: ExoPlayer, val isShowing: Boolean, val aspect: Float, val zoom: Float)

/**
 * A backdrop that fades into its trailer (tvOS `trailerOverlay`), built for the box GPU: the video
 * is a [SurfaceView], which the display composes on its own, under the [scrim]'s shades; [image],
 * the backdrop with the same shades baked in, lies over both and fades out while the trailer shows,
 * so the shades never move. Without a trailer it is [image] alone. [opacity] fades the whole
 * backdrop; below 1 the video is gone, as a SurfaceView cannot fade with it.
 */
@Composable
fun TrailerBackdrop(
    trailer: HeroTrailer,
    itemId: String,
    scrim: BakedScrim,
    modifier: Modifier = Modifier,
    opacity: () -> Float = { 1f },
    image: @Composable (Modifier) -> Unit
) {
    val picture = trailer.picture(itemId)
    val isShowing = picture?.isShowing == true
    val shown = animateFloatAsState(
        targetValue = if (isShowing) 1f else 0f,
        animationSpec = tween(if (isShowing) TrailerMotion.FADE_IN_MILLIS else TrailerMotion.FADE_OUT_MILLIS, easing = FastOutSlowInEasing),
        label = "trailer"
    )
    val imageGone by remember { derivedStateOf { shown.value >= 1f } }
    val videoDrawn by remember { derivedStateOf { opacity() >= 1f } }
    Box(modifier = modifier) {
        if (picture != null && videoDrawn) {
            HeroTrailerVideo(trailer, picture, Modifier.fillMaxSize())
            ScrimOverlay(scrim, Modifier.fillMaxSize())
        }
        if (!imageGone) {
            image(
                Modifier.fillMaxSize().graphicsLayer {
                    alpha = opacity() * (1f - shown.value)
                    // One image: alpha on its draw, no offscreen buffer while it fades.
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
            )
        }
    }
}

@Composable
private fun HeroTrailerVideo(trailer: HeroTrailer, picture: HeroTrailerPicture, modifier: Modifier) {
    val context = LocalContext.current
    val surface = remember(context) { SurfaceView(context) }
    AndroidView(
        factory = { surface },
        modifier = modifier
            .cover(picture.aspect, picture.zoom)
            .testTag(HERO_TRAILER_TAG)
            .semantics { stateDescription = if (picture.isShowing) TRAILER_PLAYING else TRAILER_HIDDEN }
    )
    DisposableEffect(trailer, surface) {
        trailer.attach(surface)
        onDispose { trailer.detach(surface) }
    }
}

/** Sizes the video to cover the box at its [aspect], [zoom] times larger, centred: the parts outside are off screen. */
private fun Modifier.cover(aspect: Float, zoom: Float): Modifier = layout { measurable, constraints ->
    val size = coverSize(constraints.maxWidth, constraints.maxHeight, aspect, zoom)
    val placeable = measurable.measure(Constraints.fixed(size.width, size.height))
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place((constraints.maxWidth - size.width) / 2, (constraints.maxHeight - size.height) / 2)
    }
}

/** The size of a video of [aspect] that covers [width] x [height], times [zoom]. */
internal fun coverSize(width: Int, height: Int, aspect: Float, zoom: Float): IntSize {
    val heightAtWidth = width / aspect
    val (coverWidth, coverHeight) = if (heightAtWidth >= height) width.toFloat() to heightAtWidth else height * aspect to height.toFloat()
    return IntSize((coverWidth * zoom).roundToInt(), (coverHeight * zoom).roundToInt())
}

/** The trailer video, for UI tests; its state description is [TRAILER_PLAYING] once it shows. */
const val HERO_TRAILER_TAG = "hero-trailer"
const val TRAILER_PLAYING = "playing"
const val TRAILER_HIDDEN = "hidden"
