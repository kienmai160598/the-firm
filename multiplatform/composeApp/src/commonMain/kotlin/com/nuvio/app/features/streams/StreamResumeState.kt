package com.nuvio.app.features.streams

import com.nuvio.app.features.watchprogress.WatchProgressEntry

internal data class StreamResumeState(
    val positionMs: Long? = null,
    val progressFraction: Float? = null,
)

internal fun resolveStreamResumeState(
    progress: WatchProgressEntry?,
    initialPositionMs: Long?,
    initialProgressFraction: Float?,
    startFromBeginning: Boolean,
): StreamResumeState {
    if (startFromBeginning || progress?.isResumable == false) return StreamResumeState()
    val fraction = (if (progress != null) progress.progressPercent?.div(100f) else initialProgressFraction)
        ?.takeIf { it > 0f }?.coerceIn(0f, 1f)
    val position = if (fraction != null) null
        else (progress?.lastPositionMs ?: initialPositionMs)?.takeIf { it > 0L }
    return StreamResumeState(positionMs = position, progressFraction = fraction)
}

internal fun Long.toPlaybackClock(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        buildString {
            append(hours)
            append(':')
            append(minutes.toString().padStart(2, '0'))
            append(':')
            append(seconds.toString().padStart(2, '0'))
        }
    } else {
        buildString {
            append(minutes)
            append(':')
            append(seconds.toString().padStart(2, '0'))
        }
    }
}
