package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.format.formatReleaseDateWithoutYear
import com.nuvio.app.core.time.daysUntilEpisodeRelease
import com.nuvio.app.core.time.parseEpisodeReleaseEpochMs

internal sealed interface AirDateBadge {
    data object Today : AirDateBadge
    data object Tomorrow : AirDateBadge
    data class InDays(val days: Int) : AirDateBadge
    data class OnDate(val formattedDate: String) : AirDateBadge
}

internal fun airDateBadge(
    releasedIso: String?,
    todayIsoDate: String,
): AirDateBadge? {
    if (releasedIso.isNullOrBlank() || todayIsoDate.isBlank()) {
        return null
    }

    val releaseEpoch = parseEpisodeReleaseEpochMs(releasedIso)
    if (releaseEpoch != null && WatchProgressClock.nowEpochMs() >= releaseEpoch) {
        return null
    }

    val daysUntil = daysUntilEpisodeRelease(
        todayIsoDate = todayIsoDate,
        releasedDate = releasedIso,
    ) ?: return null

    return when {
        daysUntil < 0 -> null
        daysUntil == 0 -> AirDateBadge.Today
        daysUntil == 1 -> AirDateBadge.Tomorrow
        daysUntil in 2..7 -> AirDateBadge.InDays(daysUntil)
        else -> AirDateBadge.OnDate(formatReleaseDateWithoutYear(releasedIso))
    }
}
