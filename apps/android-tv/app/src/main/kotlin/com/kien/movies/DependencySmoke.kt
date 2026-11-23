package com.kien.movies

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.SubtitleView
import coil3.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import fi.iki.elonen.NanoHTTPD

/**
 * Never called by the app. It references one class from each library the feature slices need, so
 * a dependency that stops resolving or loses a class breaks the shell build, not a feature slice.
 * R8 removes it from the box APK.
 */
internal object DependencySmoke {
    fun exoPlayer(context: Context): ExoPlayer = ExoPlayer.Builder(context).build()

    fun ffmpegRenderer(): FfmpegAudioRenderer = FfmpegAudioRenderer()

    fun subtitleView(context: Context): SubtitleView = SubtitleView(context)

    fun qrMatrix(text: String, size: Int): BitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)

    fun server(port: Int): NanoHTTPD = object : NanoHTTPD(port) {}

    @Composable
    fun image(url: String) {
        AsyncImage(model = url, contentDescription = null)
    }
}
