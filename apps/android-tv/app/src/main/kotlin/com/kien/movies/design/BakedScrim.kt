package com.kien.movies.design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Size
import coil3.size.pxOrElse
import coil3.transform.Transformation
import kotlin.math.roundToInt

/**
 * The darkening tvOS draws over art with gradients (HomeHeroBackdrop.swift, BackdropView.swift),
 * drawn once into the decoded bitmap instead of every frame: a Coil [Transformation], so Coil
 * caches the result and the screen draws one opaque image per frame (docs/android-tv-rebuild.md,
 * box performance rules: no full screen gradient, no stacked translucent layers).
 *
 * The bitmap is first centre cropped to the aspect of the requested size, so a stop at 0.5 lands
 * at the middle of the laid out image; draw it with `ContentScale.Crop` at that size. The art is
 * drawn over [underlay] and the result is marked opaque, so the GPU skips blending it.
 *
 * Use [HomeHero] or [Backdrop] for the tvOS looks, or build your own from [ScrimLayer]s. Build the
 * request with [bakedImageRequest] or [rememberBakedImage]; never decode the full resolution.
 */
data class BakedScrim(val underlay: Color, val layers: List<ScrimLayer>) : Transformation() {
    override val cacheKey: String =
        "BakedScrim(underlay=${underlay.toArgb()},layers=${layers.joinToString(";", transform = ScrimLayer::key)})"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val source = input.softwareCopy()
        val (width, height) = croppedSize(source.width, source.height, size)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(underlay.toArgb())
        canvas.drawBitmap(source, centreCrop(source.width, source.height, width, height), Rect(0, 0, width, height), Paint(Paint.FILTER_BITMAP_FLAG))
        val paint = Paint()
        layers.forEach { layer ->
            paint.shader = layer.shader(width.toFloat(), height.toFloat())
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
        output.setHasAlpha(false)
        return output
    }

    companion object {
        /**
         * Home's hero art (HomeHeroBackdrop.swift): leading shade for the title, bottom shade for
         * the first shelf, and the bottom 20% blended into [background], the Home window colour,
         * where tvOS masks the art to show its background.
         */
        fun homeHero(background: Color): BakedScrim = BakedScrim(
            underlay = background,
            layers = listOf(
                ScrimLayer.horizontal(0f to Color.Black.copy(alpha = 0.75f), 0.5f to Color.Black.copy(alpha = 0.25f), 0.75f to Color.Transparent),
                ScrimLayer.vertical(0.5f to Color.Transparent, 0.85f to Color.Black.copy(alpha = 0.55f), 1f to Color.Black.copy(alpha = 0.55f)),
                ScrimLayer.vertical(0.8f to background.copy(alpha = 0f), 1f to background)
            )
        )

        /**
         * The full screen art of Details and Streams (BackdropView.swift, not receded): leading
         * and bottom shades, plus a flat [dimming] like its `dimming` parameter.
         */
        fun backdrop(background: Color, dimming: Float = 0f): BakedScrim = BakedScrim(
            underlay = background,
            layers = listOfNotNull(
                if (dimming > 0f) ScrimLayer.solid(Color.Black.copy(alpha = dimming)) else null,
                ScrimLayer.horizontal(0f to Color.Black.copy(alpha = 0.9f), 0.45f to Color.Black.copy(alpha = 0.55f), 0.8f to Color.Transparent),
                ScrimLayer.vertical(0.35f to Color.Transparent, 0.8f to Color.Black.copy(alpha = 0.85f), 1f to Color.Black)
            )
        )
    }
}

/** One gradient drawn over the whole bitmap, along [axis], through [stops] (position 0 to 1). */
data class ScrimLayer(val axis: Axis, val stops: List<Pair<Float, Color>>) {
    enum class Axis { Horizontal, Vertical }

    internal val key: String get() = "$axis:" + stops.joinToString(",") { (position, color) -> "$position=${color.toArgb()}" }

    internal fun shader(width: Float, height: Float): Shader {
        val endX = if (axis == Axis.Horizontal) width else 0f
        val endY = if (axis == Axis.Vertical) height else 0f
        return LinearGradient(
            0f,
            0f,
            endX,
            endY,
            stops.map { (_, color) -> color.toArgb() }.toIntArray(),
            stops.map { (position, _) -> position }.toFloatArray(),
            Shader.TileMode.CLAMP
        )
    }

    companion object {
        fun horizontal(vararg stops: Pair<Float, Color>) = ScrimLayer(Axis.Horizontal, stops.toList())

        fun vertical(vararg stops: Pair<Float, Color>) = ScrimLayer(Axis.Vertical, stops.toList())

        fun solid(color: Color) = ScrimLayer(Axis.Vertical, listOf(0f to color, 1f to color))
    }
}

/** A request for [url] decoded at [widthPx] x [heightPx] with [scrim] baked in; null without a URL. */
fun bakedImageRequest(context: Context, url: String?, widthPx: Int, heightPx: Int, scrim: BakedScrim): ImageRequest? = url?.let { data ->
    ImageRequest.Builder(context)
        .data(data)
        .size(widthPx, heightPx)
        .transformations(scrim)
        .build()
}

/** [bakedImageRequest] at the laid out [size], remembered. */
@Composable
fun rememberBakedImage(url: String?, size: DpSize, scrim: BakedScrim): ImageRequest? {
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(url, size, scrim, density) {
        val widthPx = with(density) { size.width.roundToPx() }
        val heightPx = with(density) { size.height.roundToPx() }
        bakedImageRequest(context, url, widthPx, heightPx, scrim)
    }
}

/** The input as a software bitmap; Canvas cannot read a hardware one. */
private fun Bitmap.softwareCopy(): Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE) {
    copy(Bitmap.Config.ARGB_8888, false)
} else {
    this
}

/** The input size cut to the aspect of the requested [size]; the input size when it has none. */
private fun croppedSize(width: Int, height: Int, size: Size): Pair<Int, Int> {
    val targetWidth = size.width.pxOrElse { -1 }
    val targetHeight = size.height.pxOrElse { -1 }
    if (targetWidth <= 0 || targetHeight <= 0) return width to height
    val aspect = targetWidth.toFloat() / targetHeight
    return if (width.toFloat() / height > aspect) {
        (height * aspect).roundToInt().coerceIn(1, width) to height
    } else {
        width to (width / aspect).roundToInt().coerceIn(1, height)
    }
}

/** The centred part of a [width] x [height] bitmap with the aspect of [cropWidth] x [cropHeight]. */
private fun centreCrop(width: Int, height: Int, cropWidth: Int, cropHeight: Int): Rect {
    val left = (width - cropWidth) / 2
    val top = (height - cropHeight) / 2
    return Rect(left, top, left + cropWidth, top + cropHeight)
}
