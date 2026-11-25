package com.kien.movies.trailer

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.nuvio.tvos.TvosCancellable
import com.nuvio.tvos.TvosTrailerLink
import com.nuvio.tvos.TvosTrailers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Turns a backdrop (Home's spotlight, a Details title) into its trailer, with sound, after
 * [TrailerMotion.START_DELAY_MILLIS], and back when focus moves on, the screen goes or the trailer
 * ends (tvOS HeroTrailer.swift, AC H8 and D6). It plays hidden first while [HeroLetterbox] measures
 * its black bars, so it never shows them. Create it with [rememberHeroTrailer].
 */
@Stable
class HeroTrailer internal constructor(private val context: Context, private val scope: CoroutineScope) {
    /** What the backdrop shows of the trailer, or null when there is none. */
    var picture: HeroTrailerPicture? by mutableStateOf(null)
        private set

    private var focusedId: String? = null
    private var isDue = false
    private var player: ExoPlayer? = null
    private var surface: SurfaceView? = null
    private var waitJob: Job? = null
    private var letterboxJob: Job? = null
    private var volumeJob: Job? = null
    private var resolution: TvosCancellable? = null

    /** The trailer of [itemId] for its backdrop, or null while another title's trailer is up. */
    fun picture(itemId: String): HeroTrailerPicture? = picture?.takeIf { it.itemId == itemId }

    fun play(type: String, id: String) {
        if (id == focusedId) return
        stop()
        focusedId = id
        resolution = TvosTrailers.resolve(type, id) { link -> prepare(link, id) }
        waitJob = scope.launch {
            delay(TrailerMotion.START_DELAY_MILLIS)
            isDue = true
            player?.play()
        }
    }

    fun stop() {
        focusedId = null
        isDue = false
        waitJob?.cancel()
        resolution?.cancel()
        resolution = null
        fadeOut()
    }

    /** The screen is going: lets the player go at once, with no fade. */
    fun release() {
        stop()
        picture?.player?.takeUnless(ExoPlayer::isReleased)?.release()
        picture = null
    }

    internal fun attach(view: SurfaceView) {
        surface = view
        player?.setVideoSurfaceView(view)
    }

    internal fun detach(view: SurfaceView) {
        if (surface !== view) return
        surface = null
        picture?.player?.takeUnless(ExoPlayer::isReleased)?.clearVideoSurfaceView(view)
    }

    private fun prepare(link: TvosTrailerLink?, id: String) {
        if (focusedId != id || link == null) return
        val created = TrailerPlayer.create(context, link)
        created.addListener(Events(created))
        surface?.let(created::setVideoSurfaceView)
        player = created
        picture = HeroTrailerPicture(itemId = id, player = created, isShowing = false, aspect = DEFAULT_ASPECT, zoom = 1f)
        if (isDue) created.play()
    }

    private fun measureLetterbox(of: ExoPlayer) {
        val view = surface ?: return
        letterboxJob = scope.launch {
            val zoom = HeroLetterbox.zoom(view)
            if (player !== of) return@launch
            picture = picture?.copy(isShowing = true, zoom = zoom)
            volumeJob = scope.launch { TrailerVolume.ramp(of, to = 1f, millis = TrailerMotion.FADE_IN_MILLIS) }
        }
    }

    /** Fades picture and sound out together, then lets the player go. */
    private fun fadeOut() {
        letterboxJob?.cancel()
        volumeJob?.cancel()
        val fading = player ?: return
        player = null
        picture = picture?.copy(isShowing = false)
        scope.launch {
            try {
                TrailerVolume.ramp(fading, to = 0f, millis = TrailerMotion.FADE_OUT_MILLIS)
            } finally {
                // Also when the screen's scope is cancelled mid fade.
                if (!fading.isReleased) fading.release()
                if (picture?.player === fading) picture = null
            }
        }
    }

    private inner class Events(private val owner: ExoPlayer) : Player.Listener {
        private var measured = false

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying || measured || player !== owner) return
            measured = true
            measureLetterbox(owner)
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width <= 0 || videoSize.height <= 0 || player !== owner) return
            picture = picture?.copy(aspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && player === owner) fadeOut()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.w(TAG, "Trailer failed to load: ${error.errorCodeName}", error)
            if (player === owner) fadeOut()
        }
    }
}

private const val DEFAULT_ASPECT = 16f / 9f
private const val TAG = "HeroTrailer"
