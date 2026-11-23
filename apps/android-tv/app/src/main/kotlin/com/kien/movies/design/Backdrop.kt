package com.kien.movies.design

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest

/**
 * Full screen art behind Home's hero, Details and Streams (tvOS BackdropView.swift), rebuilt for
 * the box GPU (docs/android-tv-rebuild.md, "Not ported literally"):
 *
 * - one scrim, drawn only over the leading text band ([scrimWidthFraction] of the width), not the
 *   two full screen shades of tvOS;
 * - receded (tvOS blur 40 plus 45% black): a second, tiny decode of the same image, about 96 px
 *   wide, scaled up and darkened by a colour filter on the image itself. No blur, no extra layer.
 */
@Composable
fun Backdrop(
    url: String?,
    modifier: Modifier = Modifier,
    isReceded: Boolean = false,
    scrimWidthFraction: Float = DEFAULT_SCRIM_FRACTION
) {
    val context = LocalContext.current
    val model: Any? = remember(url, isReceded) {
        when {
            url == null -> null
            isReceded -> ImageRequest.Builder(context).data(url).size(RECEDED_WIDTH_PX, RECEDED_HEIGHT_PX).build()
            else -> url
        }
    }
    ArtworkImage(
        model = model,
        modifier = modifier.fillMaxSize().leadingScrim(if (isReceded) 0f else scrimWidthFraction),
        colorFilter = if (isReceded) recededFilter else null
    )
}

private fun Modifier.leadingScrim(widthFraction: Float): Modifier = if (widthFraction <= 0f) {
    this
} else {
    drawWithContent {
        drawContent()
        val width = size.width * widthFraction
        drawRect(brush = leadingShade(width), size = Size(width, size.height))
    }
}

// tvOS leading shade stops: black 0.9 at 0, 0.55 at 45%, clear at 80% of the band.
private fun leadingShade(width: Float): Brush = Brush.horizontalGradient(
    0f to Color.Black.copy(alpha = 0.9f),
    0.45f to Color.Black.copy(alpha = 0.55f),
    1f to Color.Transparent,
    startX = 0f,
    endX = width
)

private val recededFilter = ColorFilter.colorMatrix(
    ColorMatrix().apply { setToScale(RECEDED_BRIGHTNESS, RECEDED_BRIGHTNESS, RECEDED_BRIGHTNESS, 1f) }
)

private const val DEFAULT_SCRIM_FRACTION = 0.6f
private const val RECEDED_WIDTH_PX = 96
private const val RECEDED_HEIGHT_PX = 54
private const val RECEDED_BRIGHTNESS = 0.55f
