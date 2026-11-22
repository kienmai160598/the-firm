package com.nuvio.app.features.home.components

import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import kotlin.math.roundToInt

internal sealed interface ContinueWatchingProgressBadge {
    data class WatchedPercent(val percent: Int) : ContinueWatchingProgressBadge
    data class HoursMinutesLeft(val hours: Long, val minutes: Long) : ContinueWatchingProgressBadge
    data class MinutesLeft(val minutes: Long) : ContinueWatchingProgressBadge
}

internal fun continueWatchingProgressPercent(progressFraction: Float): Int =
    (progressFraction * 100f).roundToInt().coerceIn(1, 99)

internal fun ContinueWatchingItem.continueWatchingProgressBadge(): ContinueWatchingProgressBadge? {
    if (progressFraction > 0f) {
        if (durationMs <= 0L) {
            return ContinueWatchingProgressBadge.WatchedPercent(
                continueWatchingProgressPercent(progressFraction),
            )
        }
        val effectivePositionMs = when {
            resumePositionMs > 0L -> resumePositionMs
            else -> (durationMs * progressFraction.coerceIn(0f, 1f)).toLong()
        }
        val remainingMinutes = ((durationMs - effectivePositionMs).coerceAtLeast(0L) / 60_000L)
            .coerceAtLeast(1L)
        val hours = remainingMinutes / 60L
        val minutes = remainingMinutes % 60L
        return if (hours > 0L) {
            ContinueWatchingProgressBadge.HoursMinutesLeft(hours, minutes)
        } else {
            ContinueWatchingProgressBadge.MinutesLeft(remainingMinutes)
        }
    }

    return null
}
