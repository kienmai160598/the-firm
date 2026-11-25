package com.kien.movies.trailer

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.nuvio.app.features.trailer.YoutubeChunkedDataSourceFactory
import com.nuvio.tvos.TvosTrailerLink

/**
 * The ExoPlayer of one trailer, muted and prepared, paused until the wait is over.
 *
 * - The sharpest variant up to 1080p from the start, as tvOS cuts YouTube's HLS master playlist
 *   down to its sharpest tier: ExoPlayer would otherwise start on the lowest one.
 * - YouTube links go through [YoutubeChunkedDataSourceFactory], which YouTube does not throttle.
 * - A short buffer, for the box's 1.9 GB of RAM, and no audio focus: a trailer never pauses the
 *   music of another app.
 */
@OptIn(UnstableApi::class)
internal object TrailerPlayer {
    fun create(context: Context, link: TvosTrailerLink): ExoPlayer {
        val selector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setForceHighestSupportedBitrate(true).setMaxVideoSize(MAX_WIDTH, MAX_HEIGHT))
        }
        val buffer = DefaultLoadControl.Builder()
            .setBufferDurationsMs(MIN_BUFFER_MS, MAX_BUFFER_MS, START_BUFFER_MS, START_BUFFER_MS)
            .build()
        val audio = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build()
        val player = ExoPlayer.Builder(context)
            .setTrackSelector(selector)
            .setLoadControl(buffer)
            .setMediaSourceFactory(DefaultMediaSourceFactory(YoutubeChunkedDataSourceFactory()))
            .setAudioAttributes(audio, false)
            .build()
        val item = MediaItem.Builder().setUri(link.url)
        if (link.isHls) item.setMimeType(MimeTypes.APPLICATION_M3U8)
        player.setMediaItem(item.build())
        player.volume = 0f
        player.prepare()
        return player
    }

    private const val MAX_WIDTH = 1920
    private const val MAX_HEIGHT = 1080
    private const val MIN_BUFFER_MS = 5_000
    private const val MAX_BUFFER_MS = 15_000
    private const val START_BUFFER_MS = 1_000
}
