package com.kien.movies.addons.phone

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeTest {
    private val address = "http://192.168.1.20:8080/?t=00112233445566778899aabbccddeeff"

    /** The preview's code space on the box (242 dp at density 2), and 5b's 180 dp floor. */
    @Test
    fun thePixelsAPhoneScansDecodeToTheAddressAndFitThePreview() {
        val modules = QrCode.modules(address)
        val modulePx = QrCode.modulePixels(modules, maxSidePx = 484)
        val side = modules.width * modulePx
        val source = RGBLuminanceSource(side, side, QrCode.pixels(modules, modulePx))

        val decoded = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text

        assertTrue("side $side", side in 360..484)
        assertEquals(address, decoded)
    }
}
