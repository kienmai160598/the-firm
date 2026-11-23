package com.kien.movies.core.player

import com.kien.movies.data.repository.SkipInterval

internal fun SkipInterval.followingPostCreditsScene(
    intervals: List<SkipInterval>,
    durationMs: Long
): SkipInterval? {
    if (type != "movie-credits" && type !in PlayerNextEpisodeRules.OUTRO_SEGMENT_TYPES) return null
    val explicit = intervals.filter {
        it.type == "post-credits" && it.startTime.isFinite() && it.endTime.isFinite() &&
            it.startTime >= endTime && it.endTime > it.startTime &&
            (durationMs <= 0L || it.startTime * 1000.0 < durationMs)
    }.minByOrNull { it.startTime }
    if (explicit != null) return explicit
    if (durationMs > 0L) {
        val creditsEndMs = (endTime * 1000.0).toLong()
        val gapMs = durationMs - creditsEndMs
        if (gapMs > PlayerNextEpisodeRules.POST_OUTRO_AUTOPLAY_GAP_MS) {
            return SkipInterval(
                startTime = endTime,
                endTime = durationMs / 1000.0,
                type = "post-credits",
                provider = "heuristic"
            )
        }
    }
    return null
}
