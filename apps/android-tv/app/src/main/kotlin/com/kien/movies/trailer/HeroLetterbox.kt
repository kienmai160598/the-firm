package com.kien.movies.trailer

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import com.nuvio.tvos.TvosLetterbox
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Samples small frames from the start of a trailer and answers the zoom that crops its black bars:
 * above 1 for a letterboxed trailer, 1 when no bars held still within the sample limit (tvOS
 * HeroLetterbox.swift). The detection is Kotlin ([TvosLetterbox]); the frames come from the
 * trailer's [SurfaceView] through [PixelCopy], scaled down to the detector's sample size.
 */
internal object HeroLetterbox {
    private val main = Handler(Looper.getMainLooper())

    suspend fun zoom(surface: SurfaceView): Float {
        val detector = TvosLetterbox()
        val bitmap = Bitmap.createBitmap(detector.sampleWidth, detector.sampleHeight, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(detector.sampleWidth * detector.sampleHeight)
        try {
            repeat(detector.sampleLimit) {
                delay(detector.sampleIntervalMs)
                if (copyFrame(surface, bitmap)) {
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    detector.onFrame(pixels)?.let { zoom -> return zoom }
                }
            }
        } finally {
            bitmap.recycle()
        }
        return 1f
    }

    private suspend fun copyFrame(surface: SurfaceView, into: Bitmap): Boolean {
        if (!surface.holder.surface.isValid) return false
        return suspendCancellableCoroutine { result ->
            PixelCopy.request(surface, into, { status -> if (result.isActive) result.resume(status == PixelCopy.SUCCESS) }, main)
        }
    }
}
