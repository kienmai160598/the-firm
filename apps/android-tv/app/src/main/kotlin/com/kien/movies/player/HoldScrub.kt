package com.kien.movies.player

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Scrubbing with the D-pad (replaces tvOS MpvSwipeScrub and MpvTouchScrubber, the remote has no
 * touch surface; docs/android-tv-rebuild.md section 4, "Not ported literally"):
 * - a press of left or right skips [TAP_STEP_SECONDS] at once;
 * - holding it starts a pending scrub: playback pauses and the preview on the bar moves faster the
 *   longer the key is held, measured in time so the remote's key repeat rate does not matter;
 * - a press while a scrub is pending moves the preview by [TAP_STEP_SECONDS];
 * - [SETTLE_MILLIS] after the last key the player seeks to the preview and resumes if it was
 *   playing (tvOS e570f73af5); play/pause commits at once and resumes, Back cancels.
 */
object HoldScrub {
    const val TAP_STEP_SECONDS = 10
    const val SETTLE_MILLIS = 900L

    /** Video seconds per held second, by how long the key has been held. */
    private const val SLOW_SPEED = 20.0
    private const val MEDIUM_SPEED = 60.0
    private const val FAST_SPEED = 180.0
    private const val MEDIUM_AFTER_MILLIS = 1_000L
    private const val FAST_AFTER_MILLIS = 3_000L

    /** A short video is never crossed in less than this many held seconds. */
    private const val SHORTEST_CROSSING_SECONDS = 10.0
    private const val SLOWEST_CAP = 10.0

    /** Video seconds per held second after holding for [heldMillis], for a video of [durationSeconds]. */
    fun speed(heldMillis: Long, durationSeconds: Int): Double {
        val tier = when {
            heldMillis < MEDIUM_AFTER_MILLIS -> SLOW_SPEED
            heldMillis < FAST_AFTER_MILLIS -> MEDIUM_SPEED
            else -> FAST_SPEED
        }
        val cap = max(durationSeconds / SHORTEST_CROSSING_SECONDS, SLOWEST_CAP)
        return min(tier, cap)
    }

    /** How far one key repeat moves the preview: speed times the time since the previous key. */
    fun advanceSeconds(heldMillis: Long, sinceLastKeyMillis: Long, durationSeconds: Int): Double = speed(heldMillis, durationSeconds) * sinceLastKeyMillis.coerceAtLeast(0) / MILLIS_PER_SECOND

    private const val MILLIS_PER_SECOND = 1_000.0
}

/** A pending scrub: where it started and where the preview is, clamped to the video. */
data class ScrubSession(val originSeconds: Int, val durationSeconds: Int, val previewSeconds: Double = originSeconds.toDouble()) {
    val shownSeconds: Int get() = previewSeconds.roundToInt()

    fun moved(bySeconds: Double): ScrubSession = copy(previewSeconds = (previewSeconds + bySeconds).coerceIn(0.0, durationSeconds.toDouble()))
}

/** The skip badge over the bar: presses in the same direction add up, each one bounces it (tvOS MpvSkip). */
data class SkipBadge(val isForward: Boolean, val presses: Int) {
    fun adding(isForward: Boolean): SkipBadge = SkipBadge(isForward, if (isForward == this.isForward) presses + 1 else 1)
}
