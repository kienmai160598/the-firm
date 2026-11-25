package com.kien.movies.design

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

/**
 * The shades of [scrim] alone, without art or underlay, drawn once into a small transparent bitmap
 * and stretched over what lies under it: a trailer video, which cannot have them baked in. One
 * translucent image per frame instead of a gradient per layer.
 */
@Composable
fun ScrimOverlay(scrim: BakedScrim, modifier: Modifier = Modifier) {
    val bitmap = remember(scrim) { scrim.shadesOnly(OVERLAY_WIDTH, OVERLAY_HEIGHT).asImageBitmap() }
    Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = modifier)
}

/** The [BakedScrim.layers] on a transparent [width] x [height] bitmap. */
internal fun BakedScrim.shadesOnly(width: Int, height: Int): Bitmap {
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint()
    layers.forEach { layer ->
        paint.shader = layer.shader(width.toFloat(), height.toFloat())
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
    return output
}

// Gradients only: a quarter of the 1080p screen is enough once filtered up.
private const val OVERLAY_WIDTH = 480
private const val OVERLAY_HEIGHT = 270
