package com.nuvio.tvos.rules

import kotlin.math.abs

/**
 * When the player saves watch progress (tvOS MpvPlaybackController.swift and
 * PlaybackController.swift, docs/android-tv-rebuild.md section 3, "Progress"):
 * - every [TICK_INTERVAL_MS] while the position moved at least [MIN_TICK_ADVANCE_MS], local only;
 * - on pause, end, exit and when the app goes to the background, synced to the remote too;
 * - never before playback has started (no frame has moved yet).
 */
object ProgressSavePolicy {
    const val TICK_INTERVAL_MS = 10_000L
    const val MIN_TICK_ADVANCE_MS = 1_000L

    enum class Moment(val syncsRemote: Boolean) {
        Tick(syncsRemote = false),
        Pause(syncsRemote = true),
        End(syncsRemote = true),
        Exit(syncsRemote = true),
        Background(syncsRemote = true),
    }

    /**
     * Whether to save at [moment]. [lastTickPositionMs] is the position of the last tick save,
     * null before the first one.
     */
    fun shouldSave(
        moment: Moment,
        hasPlayed: Boolean,
        positionMs: Long,
        lastTickPositionMs: Long?,
    ): Boolean {
        if (!hasPlayed) return false
        if (moment != Moment.Tick || lastTickPositionMs == null) return true
        return abs(positionMs - lastTickPositionMs) >= MIN_TICK_ADVANCE_MS
    }

    /** The position to store: the full duration once ended, so the title counts as watched. */
    fun savedPositionMs(isEnded: Boolean, positionMs: Long, durationMs: Long): Long =
        if (isEnded) durationMs else positionMs
}
