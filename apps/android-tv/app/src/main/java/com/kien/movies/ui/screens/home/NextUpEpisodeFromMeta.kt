package com.kien.movies.ui.screens.home

import com.kien.movies.domain.model.WatchProgress
import com.kien.movies.ui.util.parseEpisodeReleaseDate
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private const val CW_NEXT_UP_NEW_SEASON_UNAIRED_WINDOW_DAYS = 7

// An episode with no date is no more watchable than one dated ahead, so a missing date counts as unaired and stays under the same setting instead of passing as aired.

internal fun isNextUpEpisodeUnaired(releaseDate: LocalDate?, today: LocalDate): Boolean = releaseDate == null || releaseDate.isAfter(today)

internal fun resolveNextUpVideoFromMeta(
    progress: WatchProgress,
    meta: CwMetaSummary,
    showUnairedNextUp: Boolean
): CwVideoSummary? {
    val episodes = meta.videos
        .filter { video ->
            val season = video.season
            val episode = video.episode
            season != null && episode != null && season != 0
        }
        .sortedWith(
            compareBy<CwVideoSummary>({ it.season ?: Int.MAX_VALUE }, {
                it.episode
                    ?: Int.MAX_VALUE
            })
        )

    if (episodes.isEmpty()) return null

    val seedSeason = progress.season
    val seedEpisode = progress.episode
    if (seedSeason == null || seedEpisode == null) return null

    var watchedIndex = episodes.indexOfFirst {
        it.season == seedSeason && it.episode == seedEpisode
    }

    // Fallback: if the seed wasn't found by season+episode
    if (watchedIndex < 0) {
        val videoId = progress.videoId.takeIf { it.isNotBlank() }
        if (videoId != null) {
            watchedIndex = episodes.indexOfFirst { it.id == videoId }
        }
        if (watchedIndex < 0) {
            // Index-based fallback: treat the seed as the Nth episode overall
            val addonSeasons = episodes.mapTo(mutableSetOf()) { it.season }
            if (seedSeason == 1 && addonSeasons.size > 1 && seedEpisode > 0) {
                val globalIndex = seedEpisode - 1 // 0-based
                if (globalIndex in episodes.indices) {
                    watchedIndex = globalIndex
                }
            }
        }
    }

    if (watchedIndex < 0) {
        return null
    }

    val todayLocal = LocalDate.now(ZoneId.systemDefault())
    val watchedEpisodeSeason = episodes[watchedIndex].season
    val nextVideo = episodes.drop(watchedIndex + 1).firstOrNull { video ->
        if (video.season in progress.excludedNextUpSeasons) return@firstOrNull false
        val releaseDate = parseEpisodeReleaseDate(video.released)
        val isSeasonRollover = video.season != watchedEpisodeSeason
        if (isSeasonRollover) {
            if (releaseDate == null) {
                return@firstOrNull false
            }
            if (!releaseDate.isAfter(todayLocal)) {
                return@firstOrNull true
            }
            // Match mobile: show unaired next-season episodes within 7-day window
            if (showUnairedNextUp) {
                val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(todayLocal, releaseDate)
                if (daysUntil <= CW_NEXT_UP_NEW_SEASON_UNAIRED_WINDOW_DAYS) {
                    return@firstOrNull true
                }
            }
            return@firstOrNull false
        }

        if (!isNextUpEpisodeUnaired(releaseDate, todayLocal)) {
            return@firstOrNull true
        }
        if (releaseDate == null && video.available == false) {
            return@firstOrNull false
        }
        showUnairedNextUp
    }

    if (nextVideo == null) {
        return null
    }

    return nextVideo
}
