package com.kien.movies.player

import android.content.Context
import android.view.Gravity
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.SubtitleView

/**
 * The video (tvOS MpvVideoView.swift): a [SurfaceView] in an [AspectRatioFrameLayout], with
 * media3's [SubtitleView] over it. A SurfaceView, not a TextureView: the decoder writes to its
 * own layer and the box's fill rate bound GPU composes nothing extra. Bars around the video are
 * black (one opaque fill, the only full screen layer of the player).
 *
 * [subtitlesLifted] moves subtitles up while the controls show (tvOS `liftSubtitles`).
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerSurface(player: ExoPlayer?, subtitlesLifted: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val views = remember(context) { VideoViews(context) }
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { views.root },
            update = { views.subtitles.setBottomPaddingFraction(if (subtitlesLifted) LIFTED_BOTTOM_FRACTION else SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION) },
            modifier = Modifier.fillMaxSize()
        )
    }
    DisposableEffect(player, views) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) = views.showAspect(videoSize)

            override fun onCues(cueGroup: CueGroup) = views.subtitles.setCues(cueGroup.cues)
        }
        player.setVideoSurfaceView(views.surface)
        player.addListener(listener)
        views.showAspect(player.videoSize)
        onDispose {
            views.subtitles.setCues(emptyList())
            if (!player.isReleased) {
                player.removeListener(listener)
                player.clearVideoSurfaceView(views.surface)
            }
        }
    }
}

/** tvOS sets mpv's sub-pos to 66 while the controls show: a third of the height from the bottom. */
private const val LIFTED_BOTTOM_FRACTION = 0.34f

@OptIn(UnstableApi::class)
private class VideoViews(context: Context) {
    val surface = SurfaceView(context).apply { keepScreenOn = true }
    val aspect = AspectRatioFrameLayout(context).apply {
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        addView(surface, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
    val subtitles = SubtitleView(context).apply {
        setUserDefaultStyle()
        setUserDefaultTextSize()
    }
    val root = FrameLayout(context).apply {
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        addView(aspect, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        addView(subtitles, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    fun showAspect(size: VideoSize) {
        if (size.width <= 0 || size.height <= 0) return
        aspect.setAspectRatio(size.width * size.pixelWidthHeightRatio / size.height)
    }
}
