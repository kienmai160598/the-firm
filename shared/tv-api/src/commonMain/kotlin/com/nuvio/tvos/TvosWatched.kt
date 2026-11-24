package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.watched.releasedEpisodesForSeason
import com.nuvio.app.features.watching.application.WatchingActions
import com.nuvio.app.features.watchprogress.CurrentDateProvider

/**
 * Mark watched and unwatched on the Details page of a series. `WatchingActions` clears the progress
 * of the episodes it marks and reconciles the series; `TvosDetails` redraws from `WatchedRepository`.
 * The Continue Watching card goes through `TvosHome.markContinueWatchingWatched`.
 */
object TvosWatched {
    private val log = Logger.withTag("TvosWatched")

    /** [isWatched] is what the card shows now; the episode flips to the other state. */
    fun toggleEpisode(id: String, videoId: String, isWatched: Boolean) {
        val meta = shownMeta(id) ?: return
        val episode = meta.videos.firstOrNull { video -> video.id == videoId }
        if (episode == null) {
            log.w { "Toggle ignored: $id has no episode $videoId" }
            return
        }
        WatchingActions.toggleEpisodeWatched(meta, episode, isCurrentlyWatched = isWatched)
        log.i { "Episode $videoId of $id marked watched=${!isWatched}" }
    }

    /** Flips every released episode of the season; [areWatched] is `TvosSeason.isWatched`. */
    fun toggleSeason(id: String, season: Int, areWatched: Boolean) {
        val meta = shownMeta(id) ?: return
        val episodes = meta.releasedEpisodesForSeason(season, CurrentDateProvider.todayIsoDate())
        if (episodes.isEmpty()) {
            log.w { "Toggle ignored: season $season of $id has no released episode" }
            return
        }
        WatchingActions.toggleSeasonWatched(meta, episodes, areCurrentlyWatched = areWatched)
        log.i { "Season $season of $id (${episodes.size} episodes) marked watched=${!areWatched}" }
    }

    private fun shownMeta(id: String): MetaDetails? {
        val meta = MetaDetailsRepository.uiState.value.meta?.takeIf { it.id == id }
        if (meta == null) log.w { "Watched toggle ignored: details for $id are not loaded" }
        return meta
    }
}
