package com.kien.movies.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil3.size.Size
import coil3.size.pxOrElse
import coil3.transform.Transformation
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The Continue Watching art (spec section 3, "Cards"; `03-sidebar-down.png`): 16:9 art on top of a
 * card that is taller than 16:9, with the art's last rows stretched down into the bottom band and
 * a shade over the band, so the band's text reads. Drawn once into the decoded bitmap (a Coil
 * [Transformation], cached by Coil), so a card draws one opaque image per frame, no blur and no
 * extra layer (box GPU rules).
 *
 * The output has the requested size's aspect; request the card's size and draw it with
 * `ContentScale.Crop`.
 */
internal data class BandExtension(val artAspect: Float, val shade: Color, val underlay: Color) : Transformation() {
    override val cacheKey: String = "BandExtension(artAspect=$artAspect,shade=${shade.toArgb()},underlay=${underlay.toArgb()})"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && input.config == Bitmap.Config.HARDWARE) input.copy(Bitmap.Config.ARGB_8888, false) else input
        val width = size.width.pxOrElse { source.width }
        val height = size.height.pxOrElse { (source.width / artAspect).roundToInt() }
        val artHeight = (width / artAspect).roundToInt().coerceIn(1, height)
        val crop = centreCrop(source.width, source.height, artAspect)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawColor(underlay.toArgb())
        canvas.drawBitmap(source, crop, Rect(0, 0, width, artHeight), paint)
        if (artHeight < height) {
            val stripRows = max(1, (crop.height() * STRIP_FRACTION).roundToInt())
            val strip = Rect(crop.left, crop.bottom - stripRows, crop.right, crop.bottom)
            canvas.drawBitmap(source, strip, Rect(0, artHeight, width, height), paint)
        }
        val shadeTop = (artHeight - (height - artHeight)).coerceAtLeast(0).toFloat()
        paint.shader = LinearGradient(0f, shadeTop, 0f, height.toFloat(), Color.Transparent.toArgb(), shade.toArgb(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, shadeTop, width.toFloat(), height.toFloat(), paint)
        output.setHasAlpha(false)
        return output
    }

    private companion object {
        /** Share of the art's height that is stretched into the band. */
        const val STRIP_FRACTION = 0.04f
    }
}

/** The centred part of a [width] x [height] bitmap with [aspect] (width / height). */
private fun centreCrop(width: Int, height: Int, aspect: Float): Rect = if (width.toFloat() / height > aspect) {
    val cropWidth = (height * aspect).roundToInt().coerceIn(1, width)
    val left = (width - cropWidth) / 2
    Rect(left, 0, left + cropWidth, height)
} else {
    val cropHeight = (width / aspect).roundToInt().coerceIn(1, height)
    val top = (height - cropHeight) / 2
    Rect(0, top, width, top + cropHeight)
}
