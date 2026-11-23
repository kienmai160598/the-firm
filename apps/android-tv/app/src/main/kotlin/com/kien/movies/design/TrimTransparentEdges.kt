package com.kien.movies.design

import android.graphics.Bitmap
import android.os.Build
import coil3.size.Size
import coil3.transform.Transformation

/**
 * Crops the fully transparent margin off a title logo, so the logo's ink, not its canvas, fits the
 * logo box and sits on the box's bottom edge (spec section 3, "Hero": Apple's logos are tight to
 * their ink). Runs once per decode; Coil caches the result.
 */
object TrimTransparentEdges : Transformation() {
    override val cacheKey: String = "TrimTransparentEdges(alpha=$ALPHA_THRESHOLD)"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && input.config == Bitmap.Config.HARDWARE) input.copy(Bitmap.Config.ARGB_8888, false) else input
        if (!source.hasAlpha()) return source
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        var left = width
        var top = height
        var right = -1
        var bottom = -1
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if (pixels[row + x] ushr ALPHA_SHIFT > ALPHA_THRESHOLD) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    bottom = y
                }
            }
        }
        val isBlank = right < 0
        val isTight = left == 0 && top == 0 && right == width - 1 && bottom == height - 1
        return if (isBlank || isTight) source else Bitmap.createBitmap(source, left, top, right - left + 1, bottom - top + 1)
    }

    private const val ALPHA_SHIFT = 24
    private const val ALPHA_THRESHOLD = 8
}
