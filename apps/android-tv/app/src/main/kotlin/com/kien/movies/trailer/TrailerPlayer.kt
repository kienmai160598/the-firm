package com.kien.movies.trailer

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.nuvio.app.features.trailer.YoutubeChunkedDataSourceFactory
import com.nuvio.tvos.TvosTrailerLink

/**
 * The ExoPlayer of one trailer, muted and prepared, paused until the wait is over.
 *
 * - The sharpest variant the display shows from the start, as tvOS cuts YouTube's HLS master
 *   playlist down to its sharpest tier: ExoPlayer would otherwise start on the lowest one. The
 *   track selector limits it to the display's mode (its default viewport), so a 4K TV gets
 *   YouTube's 2160p VP9 when its decoder plays it and a 1080p box stays at 1080p.
 * - A direct YouTube file goes through [YoutubeChunkedDataSourceFactory], which YouTube does not
 *   throttle. An HLS link does not: the factory adds a `range` query to every googlevideo.com
 *   request, and YouTube answers an HLS segment that carries one with 400.
 * - A short buffer, for the box's 1.9 GB of RAM, and no audio focus: a trailer never pauses the
 *   music of another app.
 */
@OptIn(UnstableApi::class)
internal object TrailerPlayer {
    fun create(context: Context, link: TvosTrailerLink): ExoPlayer {
        val selector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setForceHighestSupportedBitrate(true))
        }
        val buffer = DefaultLoadControl.Builder()
            .setBufferDurationsMs(MIN_BUFFER_MS, MAX_BUFFER_MS, START_BUFFER_MS, START_BUFFER_MS)
            .build()
        val audio = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build()
        val player = ExoPlayer.Builder(context)
            .setTrackSelector(selector)
            .setLoadControl(buffer)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource(link)))
            .setAudioAttributes(audio, false)
            .build()
        val item = MediaItem.Builder().setUri(link.url)
        if (link.isHls) item.setMimeType(MimeTypes.APPLICATION_M3U8)
        player.setMediaItem(item.build())
        player.volume = 0f
        player.addAnalyticsListener(LogsVideoFormat)
        player.prepare()
        return player
    }

    private fun dataSource(link: TvosTrailerLink): DataSource.Factory = if (link.isHls) DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true) else YoutubeChunkedDataSourceFactory()

    /** The variant the selector took, as tvOS logs "Trailer shows at". */
    private object LogsVideoFormat : AnalyticsListener {
        override fun onVideoInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {
            Log.i(LOG_TAG, "Trailer plays at ${format.width}x${format.height} ${format.sampleMimeType}")
        }
    }

    private const val LOG_TAG = "TrailerPlayer"
    private const val MIN_BUFFER_MS = 5_000
    private const val MAX_BUFFER_MS = 15_000
    private const val START_BUFFER_MS = 1_000
}
