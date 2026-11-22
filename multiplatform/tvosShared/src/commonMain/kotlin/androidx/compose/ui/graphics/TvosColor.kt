package androidx.compose.ui.graphics

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline
import kotlin.math.cbrt
import kotlin.math.pow

private const val CHANNEL_MAX = 0xFF
private const val ARGB_SHIFT = 32

@Immutable
@JvmInline
value class Color(val value: ULong) {
    val alpha: Float get() = channel(24)
    val red: Float get() = channel(16)
    val green: Float get() = channel(8)
    val blue: Float get() = channel(0)

    fun copy(
        alpha: Float = this.alpha,
        red: Float = this.red,
        green: Float = this.green,
        blue: Float = this.blue,
    ): Color = Color(red, green, blue, alpha)

    private fun channel(shift: Int): Float =
        ((value shr (ARGB_SHIFT + shift)).toInt() and CHANNEL_MAX) / CHANNEL_MAX.toFloat()

    companion object {
        val Black = Color(0xFF000000)
        val White = Color(0xFFFFFFFF)
        val Transparent = Color(0x00000000)
    }
}

fun Color(color: Long): Color = Color((color.toULong() and 0xFFFFFFFFUL) shl ARGB_SHIFT)

fun Color(color: Int): Color = Color(color.toLong() and 0xFFFFFFFFL)

fun Color(red: Float, green: Float, blue: Float, alpha: Float = 1f): Color =
    Color(packArgb(toByte(alpha), toByte(red), toByte(green), toByte(blue)))

fun Color.toArgb(): Int = (value shr ARGB_SHIFT).toInt()

private fun toByte(channel: Float): Int = (channel.coerceIn(0f, 1f) * CHANNEL_MAX + 0.5f).toInt()

private fun packArgb(alpha: Int, red: Int, green: Int, blue: Int): Long =
    ((alpha and CHANNEL_MAX).toLong() shl 24) or
        ((red and CHANNEL_MAX).toLong() shl 16) or
        ((green and CHANNEL_MAX).toLong() shl 8) or
        (blue and CHANNEL_MAX).toLong()

fun Color.luminance(): Float =
    (0.2126f * linear(red) + 0.7152f * linear(green) + 0.0722f * linear(blue)).coerceIn(0f, 1f)

fun lerp(start: Color, stop: Color, fraction: Float): Color {
    val from = start.toOklab()
    val to = stop.toOklab()
    val mixed = FloatArray(3) { index -> from[index] + (to[index] - from[index]) * fraction }
    val alpha = start.alpha + (stop.alpha - start.alpha) * fraction
    return fromOklab(mixed, alpha.coerceIn(0f, 1f))
}

private fun linear(channel: Float): Float =
    if (channel <= 0.04045f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)

private fun gamma(channel: Float): Float =
    if (channel <= 0.0031308f) channel * 12.92f else 1.055f * channel.pow(1f / 2.4f) - 0.055f

private fun Color.toOklab(): FloatArray {
    val r = linear(red)
    val g = linear(green)
    val b = linear(blue)
    val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
    val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
    val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
    return floatArrayOf(
        0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
        1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
        0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s,
    )
}

private fun fromOklab(lab: FloatArray, alpha: Float): Color {
    val l = (lab[0] + 0.3963377774f * lab[1] + 0.2158037573f * lab[2]).pow(3)
    val m = (lab[0] - 0.1055613458f * lab[1] - 0.0638541728f * lab[2]).pow(3)
    val s = (lab[0] - 0.0894841775f * lab[1] - 1.2914855480f * lab[2]).pow(3)
    val r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
    val g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
    val b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
    return Color(
        gamma(r.coerceIn(0f, 1f)),
        gamma(g.coerceIn(0f, 1f)),
        gamma(b.coerceIn(0f, 1f)),
        alpha,
    )
}
