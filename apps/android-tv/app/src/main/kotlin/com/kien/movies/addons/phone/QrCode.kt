package com.kien.movies.addons.phone

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * The QR code of "Add from phone": black modules on white with the 4 module quiet zone, drawn
 * once into a bitmap at a whole number of pixels per module (crisp edges, no filtering) and then
 * only blitted. At least [minSide] across so a phone reads it from the sofa.
 */
@Composable
fun QrCodeImage(content: String, minSide: Dp, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val minSidePx = with(density) { minSide.roundToPx() }
    val image = remember(content, minSidePx) { QrCode.image(content, minSidePx) }
    val side = with(density) { image.width.toDp() }
    Image(
        bitmap = image,
        contentDescription = "QR code for $content",
        filterQuality = FilterQuality.None,
        modifier = modifier.size(side)
    )
}

object QrCode {
    private const val QUIET_ZONE_MODULES = 4
    private const val BLACK = 0xFF000000.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()

    /** One bit per module, quiet zone included. */
    fun modules(content: String): BitMatrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(
            EncodeHintType.MARGIN to QUIET_ZONE_MODULES,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
    )

    /** Whole pixels per module so the side is at least [minSidePx]. */
    fun modulePixels(modules: BitMatrix, minSidePx: Int): Int = maxOf(1, (minSidePx + modules.width - 1) / modules.width)

    /** ARGB pixels, row by row, of [modules] at [modulePx] pixels per module. */
    fun pixels(modules: BitMatrix, modulePx: Int): IntArray {
        val side = modules.width * modulePx
        return IntArray(side * side) { index ->
            val x = index % side / modulePx
            val y = index / side / modulePx
            if (modules[x, y]) BLACK else WHITE
        }
    }

    fun image(content: String, minSidePx: Int): ImageBitmap {
        val modules = modules(content)
        val modulePx = modulePixels(modules, minSidePx)
        val side = modules.width * modulePx
        return Bitmap.createBitmap(pixels(modules, modulePx), side, side, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
}
