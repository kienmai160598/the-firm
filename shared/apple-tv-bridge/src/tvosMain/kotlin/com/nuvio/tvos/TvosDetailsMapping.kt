package com.nuvio.tvos

import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaDetailsUiState
import com.nuvio.app.features.details.MetaPerson
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.details.SPECIALS_SEASON_NUMBER
import com.nuvio.app.features.details.SeriesPrimaryAction
import com.nuvio.app.features.details.formatMetaReleaseLineForDetails
import com.nuvio.app.features.details.formatRuntimeForDisplay
import com.nuvio.app.features.details.formatRuntimeFromMinutes
import com.nuvio.app.features.details.groupedEpisodesForDisplay
import com.nuvio.app.features.details.isReleasedBy
import com.nuvio.app.features.details.seriesPrimaryAction
import com.nuvio.app.features.watched.WatchedUiState
import com.nuvio.app.features.watching.application.WatchingState
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesUiState
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.WatchProgressUiState
import com.nuvio.app.features.watchprogress.buildPlaybackVideoId
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_play
import nuvio.composeapp.generated.resources.action_resume
import nuvio.composeapp.generated.resources.compose_player_episode_code_episode_only
import nuvio.composeapp.generated.resources.compose_player_episode_code_full
import nuvio.composeapp.generated.resources.episodes_season
import nuvio.composeapp.generated.resources.episodes_specials
import org.jetbrains.compose.resources.getString

private const val CAST_LIMIT = 12

private data class WatchContext(
    val progress: WatchProgressUiState,
    val watched: WatchedUiState,
    val preferences: ContinueWatchingPreferencesUiState,
    val todayIsoDate: String,
)

internal suspend fun tvosDetailsState(
    state: MetaDetailsUiState,
    progress: WatchProgressUiState,
    watched: WatchedUiState,
    preferences: ContinueWatchingPreferencesUiState,
): TvosDetailsState {
    val context = WatchContext(progress, watched, preferences, CurrentDateProvider.todayIsoDate())
    return TvosDetailsState(
        details = state.meta?.toTvosMetaDetails(context),
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
    )
}

private suspend fun MetaDetails.toTvosMetaDetails(context: WatchContext): TvosMetaDetails =
    TvosMetaDetails(
        metaId = id,
        type = type,
        name = name,
        logoUrl = logo?.takeIf(String::isNotBlank),
        backdropUrl = (background ?: poster)?.takeIf(String::isNotBlank),
        posterUrl = poster?.takeIf(String::isNotBlank),
        releaseLine = formatMetaReleaseLineForDetails(this),
        runtime = formatRuntimeForDisplay(runtime)?.takeIf(String::isNotBlank),
        ageRating = ageRating?.takeIf(String::isNotBlank),
        imdbRating = imdbRating?.takeIf(String::isNotBlank),
        genres = genres,
        overview = description?.takeIf(String::isNotBlank),
        directors = director,
        cast = cast.take(CAST_LIMIT).map(MetaPerson::toTvosCastMember),
        seasons = tvosSeasons(context),
        primaryAction = primaryAction(context),
    )

private fun MetaPerson.toTvosCastMember(): TvosCastMember =
    TvosCastMember(
        name = name,
        role = role?.takeIf(String::isNotBlank),
        photoUrl = photo?.takeIf(String::isNotBlank),
    )

private suspend fun MetaDetails.tvosSeasons(context: WatchContext): List<TvosSeason> {
    if (type != "series") return emptyList()
    val progressByVideoId = context.progress.byVideoIdForContent(id)
    return groupedEpisodesForDisplay().map { (seasonNumber, videos) ->
        TvosSeason(
            number = seasonNumber,
            title = seasonTitle(seasonNumber),
            episodes = videos.map { video ->
                toTvosEpisode(video, context, progressByVideoId[episodeProgressVideoId(video)])
            },
        )
    }
}

private fun MetaDetails.episodeProgressVideoId(video: MetaVideo): String =
    buildPlaybackVideoId(
        parentMetaId = id,
        seasonNumber = video.season,
        episodeNumber = video.episode,
        fallbackVideoId = video.id,
    )

private suspend fun seasonTitle(seasonNumber: Int): String =
    if (seasonNumber == SPECIALS_SEASON_NUMBER) {
        getString(Res.string.episodes_specials)
    } else {
        getString(Res.string.episodes_season, seasonNumber)
    }

private suspend fun MetaDetails.toTvosEpisode(
    video: MetaVideo,
    context: WatchContext,
    progressEntry: WatchProgressEntry?,
): TvosEpisode {
    val code = video.episode
        ?.let { episode -> getString(Res.string.compose_player_episode_code_episode_only, episode) }
        .orEmpty()
    return TvosEpisode(
        videoId = video.id,
        code = code,
        title = video.title,
        overview = video.overview?.takeIf(String::isNotBlank),
        thumbnailUrl = video.thumbnail?.takeIf(String::isNotBlank),
        releaseDate = video.released
            ?.takeIf(String::isNotBlank)
            ?.let(::formatReleaseDateForDisplay),
        runtime = video.runtime?.let(::formatRuntimeFromMinutes)?.takeIf(String::isNotBlank),
        isReleased = video.isReleasedBy(context.todayIsoDate),
        isWatched = progressEntry?.isEffectivelyCompleted == true ||
            WatchingState.isEpisodeWatched(context.watched.watchedKeys, type, id, video),
        progressFraction = progressEntry
            ?.takeIf { entry -> entry.durationMs > 0L && !entry.isCompleted }
            ?.progressFraction
            ?: 0f,
        streamTarget = episodeStreamTarget(video),
    )
}

internal suspend fun episodeSubtitle(season: Int?, episode: Int?, title: String?): String? {
    val episodeCode = if (season != null && episode != null) {
        getString(Res.string.compose_player_episode_code_full, season, episode)
    } else {
        null
    }
    return listOfNotNull(episodeCode, title?.takeIf(String::isNotBlank))
        .joinToString(" ")
        .ifBlank { null }
}

private suspend fun MetaDetails.episodeStreamTarget(video: MetaVideo): TvosStreamTarget =
    TvosStreamTarget(
        type = type,
        videoId = video.id,
        parentMetaId = id,
        seasonNumber = video.season,
        episodeNumber = video.episode,
        title = name,
        subtitle = episodeSubtitle(video.season, video.episode, video.title),
        backdropUrl = (background ?: video.thumbnail ?: poster)?.takeIf(String::isNotBlank),
        posterUrl = poster?.takeIf(String::isNotBlank),
        logoUrl = logo?.takeIf(String::isNotBlank),
        episodeTitle = video.title.takeIf(String::isNotBlank),
        episodeThumbnailUrl = video.thumbnail?.takeIf(String::isNotBlank),
        genres = genres,
        ageRating = ageRating?.takeIf(String::isNotBlank),
    )

private suspend fun MetaDetails.actionStreamTarget(action: SeriesPrimaryAction): TvosStreamTarget =
    TvosStreamTarget(
        type = type,
        videoId = action.videoId,
        parentMetaId = id,
        seasonNumber = action.seasonNumber,
        episodeNumber = action.episodeNumber,
        title = name,
        subtitle = episodeSubtitle(action.seasonNumber, action.episodeNumber, action.episodeTitle),
        backdropUrl = (background ?: action.episodeThumbnail ?: poster)?.takeIf(String::isNotBlank),
        posterUrl = poster?.takeIf(String::isNotBlank),
        logoUrl = logo?.takeIf(String::isNotBlank),
        episodeTitle = action.episodeTitle?.takeIf(String::isNotBlank),
        episodeThumbnailUrl = action.episodeThumbnail?.takeIf(String::isNotBlank),
        genres = genres,
        ageRating = ageRating?.takeIf(String::isNotBlank),
    )

private fun MetaDetails.movieStreamTarget(): TvosStreamTarget =
    TvosStreamTarget(
        type = type,
        videoId = id,
        parentMetaId = id,
        seasonNumber = null,
        episodeNumber = null,
        title = name,
        subtitle = null,
        backdropUrl = (background ?: poster)?.takeIf(String::isNotBlank),
        posterUrl = poster?.takeIf(String::isNotBlank),
        logoUrl = logo?.takeIf(String::isNotBlank),
        episodeTitle = null,
        episodeThumbnailUrl = null,
        genres = genres,
        ageRating = ageRating?.takeIf(String::isNotBlank),
    )

private suspend fun MetaDetails.primaryAction(context: WatchContext): TvosPlayAction? =
    if (type == "series") seriesPlayAction(context) else moviePlayAction(context.progress)

private suspend fun MetaDetails.seriesPlayAction(context: WatchContext): TvosPlayAction? {
    val action = seriesPrimaryAction(
        entries = context.progress.entries,
        watchedItems = context.watched.items,
        todayIsoDate = context.todayIsoDate,
        preferFurthestEpisode = context.preferences.upNextFromFurthestEpisode,
        watchedKeys = context.watched.watchedKeys,
        allowRewatch = true,
    ) ?: return null
    val target = videoFor(action)?.let { video -> episodeStreamTarget(video) }
        ?: actionStreamTarget(action)
    val resume = action.resumePositionMs?.let {
        tvosResumePoint(
            context.progress.progressForVideo(
                videoId = target.videoId,
                parentMetaId = id,
                seasonNumber = target.seasonNumber,
                episodeNumber = target.episodeNumber,
            ),
        )
    }
    return TvosPlayAction(label = action.label, target = target, resume = resume)
}

private fun MetaDetails.videoFor(action: SeriesPrimaryAction): MetaVideo? {
    val season = action.seasonNumber
    val episode = action.episodeNumber
    if (season != null && episode != null) {
        return videos.firstOrNull { video -> video.season == season && video.episode == episode }
    }
    return videos.firstOrNull { video -> video.id == action.videoId }
}

private suspend fun MetaDetails.moviePlayAction(progress: WatchProgressUiState): TvosPlayAction {
    val resume = tvosResumePoint(progress.byVideoIdForContent(id)[id])
    val label = getString(if (resume != null) Res.string.action_resume else Res.string.action_play)
    return TvosPlayAction(label = label, target = movieStreamTarget(), resume = resume)
}
