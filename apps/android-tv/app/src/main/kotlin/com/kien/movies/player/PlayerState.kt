package com.kien.movies.player

/**
 * Where one playback is (docs/android-tv-rebuild.md, section 3, "Player"):
 * `Loading -> Playing <-> Paused`, `Playing <-> Buffering`, then `Ended` or `Failed`, both final.
 */
enum class PlaybackPhase {
    Loading,
    Playing,
    Paused,
    Buffering,
    Ended,
    Failed
}

/**
 * The player state machine, free of ExoPlayer types so it is unit tested (tvOS
 * MpvPlaybackController.swift keeps the same flags).
 *
 * @property hasStarted the engine was ready once and the start position was applied; commands
 *   (pause, seek, tracks) are ignored before.
 * @property hasPlayed frames moved once; only then is progress saved and the advisory shown.
 * @property isPausedByViewer paused after playback started (shows the pause icon on the bar).
 */
data class PlayerState(
    val phase: PlaybackPhase = PlaybackPhase.Loading,
    val hasStarted: Boolean = false,
    val hasPlayed: Boolean = false,
    val isPausedByViewer: Boolean = false,
    val failure: String? = null
) {
    val isFinal: Boolean get() = phase == PlaybackPhase.Ended || phase == PlaybackPhase.Failed

    val isPaused: Boolean get() = phase == PlaybackPhase.Paused

    /** The spinner shows while loading or buffering. */
    val isWaiting: Boolean get() = phase == PlaybackPhase.Loading || phase == PlaybackPhase.Buffering

    fun on(event: PlayerEvent): PlayerState {
        if (isFinal) return this
        return when (event) {
            PlayerEvent.Started -> copy(hasStarted = true)
            is PlayerEvent.Engine -> engine(event)
            is PlayerEvent.Ended -> ended(event)
            is PlayerEvent.Failed -> copy(phase = PlaybackPhase.Failed, failure = event.message)
        }
    }

    private fun engine(event: PlayerEvent.Engine): PlayerState = when {
        !hasStarted -> copy(phase = PlaybackPhase.Loading)
        event.isPlaying -> copy(phase = PlaybackPhase.Playing, hasPlayed = true, isPausedByViewer = false)
        !event.playWhenReady -> copy(phase = PlaybackPhase.Paused, isPausedByViewer = hasPlayed)
        event.isBuffering -> copy(phase = if (hasPlayed) PlaybackPhase.Buffering else PlaybackPhase.Loading)
        else -> this
    }

    private fun ended(event: PlayerEvent.Ended): PlayerState {
        val isNearEnd = event.durationMs <= 0 || event.positionMs >= event.durationMs - END_TOLERANCE_MS
        if (!isNearEnd) return copy(phase = PlaybackPhase.Failed, failure = STOPPED_EARLY)
        return copy(phase = PlaybackPhase.Ended, isPausedByViewer = false)
    }

    companion object {
        /** The engine reports the end this close to the duration and it counts as the end. */
        const val END_TOLERANCE_MS = 5_000L
        const val STOPPED_EARLY = "The stream stopped sending data before the end."
    }
}

/** What the engine reported, mapped from ExoPlayer callbacks by [PlayerController]. */
sealed interface PlayerEvent {
    /** The first ready state was handled: the start position is applied and play was asked for. */
    data object Started : PlayerEvent

    data class Engine(val isBuffering: Boolean, val playWhenReady: Boolean, val isPlaying: Boolean) : PlayerEvent

    data class Ended(val positionMs: Long, val durationMs: Long) : PlayerEvent

    data class Failed(val message: String) : PlayerEvent
}
