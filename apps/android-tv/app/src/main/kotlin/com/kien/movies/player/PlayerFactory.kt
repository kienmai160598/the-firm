package com.kien.movies.player

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory

/**
 * Builds the ExoPlayer of one playback (docs/android-tv-rebuild.md, section 5): the media3 fork
 * plus the app's downmix build of the FFmpeg audio decoder.
 *
 * Extension renderer mode ON with decoder fallback, the old app's default (`decoderPriority = 1`
 * in its PlayerSettingsDataStore): the box's MediaCodec decoders first, FFmpeg for what they
 * cannot decode (DTS, TrueHD on most boxes). The FFmpeg renderer keeps the source channel layout
 * (the old app's default, downmix off); a downmix setting is a later slice.
 */
@OptIn(UnstableApi::class)
internal object PlayerFactory {
    fun create(context: Context, dataSourceFactory: DataSource.Factory): ExoPlayer {
        // Constant bitrate seeking lets MKV, TS and MP3 files without an index seek at all.
        val extractors = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        val renderers = PlayerRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        return ExoPlayer.Builder(context, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory, extractors))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}

/** Sets the FFmpeg audio renderer, when the factory adds one, to keep the source layout. */
@OptIn(UnstableApi::class)
private class PlayerRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>
    ) {
        val firstNew = out.size
        super.buildAudioRenderers(context, extensionRendererMode, mediaCodecSelector, enableDecoderFallback, audioSink, eventHandler, eventListener, out)
        out.subList(firstNew, out.size).filterIsInstance<FfmpegAudioRenderer>().forEach { ffmpeg ->
            ffmpeg.setAudioOutputChannels(null, 0)
            ffmpeg.setDownmixNormalizationEnabled(false)
        }
    }
}
