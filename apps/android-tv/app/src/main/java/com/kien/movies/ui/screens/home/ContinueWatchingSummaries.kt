package com.kien.movies.ui.screens.home

import com.kien.movies.core.util.isEpisodeReleaseAired
import com.kien.movies.core.util.parseEpisodeReleaseInstant
import com.kien.movies.domain.model.Meta
import com.kien.movies.ui.util.parseEpisodeReleaseDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * Lightweight projection of [Meta] for CW pipeline caching.
 * Drops cast, crew, trailers, streams, release dates, and other heavy fields
 * that are never read by continue-watching or badge evaluation.
 */
internal data class CwMetaSummary(
    val id: String,
    val name: String,
    val poster: String?,
    val backdropUrl: String?,
    val logo: String?,
    val description: String?,
    val genres: List<String>,
    val releaseInfo: String?,
    val imdbRating: Float?,
    val language: String?,
    val country: String?,
    val videos: List<CwVideoSummary>
) {
    fun watchableEpisodes(): List<CwVideoSummary> {
        val candidates = videos.filter {
            it.season != null && it.episode != null &&
                (it.season ?: 0) > 0
        }
        fun isFutureRelease(raw: String?): Boolean = isEpisodeReleaseAired(raw) == false
        val unavailableSeasons = candidates.groupBy { it.season }
            .filter { (_, eps) ->
                val first = eps.minByOrNull { it.episode ?: Int.MAX_VALUE } ?: return@filter false
                if (first.available == false) return@filter true
                isFutureRelease(first.released)
            }.keys
        return candidates
            .filter { it.season !in unavailableSeasons }
            .filter { it.available != false && !isFutureRelease(it.released) }
    }

    /**
     * Returns the start-of-day (00:00 UTC) epochMs of the earliest upcoming season's
     * first episode release date minus 7 days, or null if no upcoming seasons are known.
     * Subtracts 7 days so revalidation triggers a week before the premiere,
     * allowing the countdown badge ("Airs in X days") to appear in CW.
     * Uses start-of-day so revalidation triggers right after midnight, not at
     * the exact broadcast time.
     */
    fun earliestUpcomingSeasonMs(): Long? {
        val today = java.time.LocalDate.now()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        val candidates = videos.filter { (it.season ?: 0) > 0 }
        return candidates.groupBy { it.season }
            .mapNotNull { (_, eps) ->
                val first =
                    eps.minByOrNull { it.episode ?: Int.MAX_VALUE } ?: return@mapNotNull null
                if (first.available == false) return@mapNotNull null
                val date = parseEpisodeReleaseDate(first.released)
                if (date == null) return@mapNotNull null
                if (date.isAfter(today)) {
                    val premiereMs = date.atStartOfDay(
                        java.time.ZoneId.systemDefault()
                    ).toInstant().toEpochMilli()
                    // Trigger revalidation 7 days before premiere so countdown badge shows up
                    (premiereMs - sevenDaysMs).coerceAtLeast(System.currentTimeMillis())
                } else {
                    null
                }
            }
            .minOrNull()
    }

    /**
     * Earliest not-yet-aired episode release instant (mid-season or new season).
     * Used as a revalidation deadline so currently-airing "caught up" series are
     * re-checked the day a new episode drops, instead of waiting the default 7-day TTL.
     */
    fun earliestUpcomingEpisodeMs(now: Instant = Instant.now()): Long? {
        return videos
            .asSequence()
            .filter { (it.season ?: 0) > 0 && it.available != false }
            .mapNotNull { video -> parseEpisodeReleaseInstant(video.released) }
            .filter { it.isAfter(now) }
            .minOrNull()
            ?.toEpochMilli()
    }

    /** Best revalidation deadline: next episode air time, else next-season window. */
    fun earliestRevalidationMs(now: Instant = Instant.now()): Long? = listOfNotNull(earliestUpcomingEpisodeMs(now), earliestUpcomingSeasonMs()).minOrNull()
}

internal data class CwVideoSummary(
    val id: String,
    val title: String?,
    val released: String?,
    val thumbnail: String?,
    val season: Int?,
    val episode: Int?,
    val overview: String?,
    val available: Boolean? = null,
    val rating: Double? = null
)

internal data class NextUpResolution(
    val season: Int,
    val episode: Int,
    val videoId: String,
    val episodeTitle: String?,
    val released: String?,
    val hasAired: Boolean,
    val airDateLabel: String?,
    val lastWatched: Long
)
