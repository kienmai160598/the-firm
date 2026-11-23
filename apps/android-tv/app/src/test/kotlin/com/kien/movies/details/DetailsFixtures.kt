package com.kien.movies.details

import com.nuvio.tvos.TvosCastMember
import com.nuvio.tvos.TvosEpisode
import com.nuvio.tvos.TvosMetaDetails
import com.nuvio.tvos.TvosPlayAction
import com.nuvio.tvos.TvosResumePoint
import com.nuvio.tvos.TvosSeason
import com.nuvio.tvos.TvosStreamTarget

// Bridge values with defaults, so each test shows only the fields it depends on.

internal fun target(seasonNumber: Int? = null, episodeNumber: Int? = null) = TvosStreamTarget(
    type = "series",
    videoId = "show:1:1",
    parentMetaId = "show",
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    title = "Show",
    subtitle = null,
    backdropUrl = null,
    posterUrl = null,
    logoUrl = null,
    episodeTitle = null,
    episodeThumbnailUrl = null
)

internal fun playAction(seasonNumber: Int? = null, resume: TvosResumePoint? = null) = TvosPlayAction(label = "Play", target = target(seasonNumber), resume = resume)

internal fun resumePoint(label: String = "Resume from 12:00", fraction: Float = 0.5f) = TvosResumePoint(label = label, startOverLabel = "Start Over", watchedFraction = fraction)

internal fun season(number: Int) = TvosSeason(number = number, title = "Season $number", episodes = emptyList())

internal fun episode(
    code: String = "E1",
    title: String = "Pilot",
    isReleased: Boolean = true,
    isWatched: Boolean = false,
    progressFraction: Float = 0f,
    releaseDate: String? = null,
    runtime: String? = null,
    episodeNumber: Int? = 1
) = TvosEpisode(
    videoId = "show:1:1",
    code = code,
    title = title,
    overview = null,
    thumbnailUrl = null,
    releaseDate = releaseDate,
    runtime = runtime,
    isReleased = isReleased,
    isWatched = isWatched,
    progressFraction = progressFraction,
    streamTarget = target(1, episodeNumber)
)

internal fun details(
    name: String = "Show",
    type: String = "series",
    seasons: List<TvosSeason> = emptyList(),
    primaryAction: TvosPlayAction? = null,
    genres: List<String> = emptyList(),
    releaseLine: String? = null,
    runtime: String? = null,
    ageRating: String? = null,
    directors: List<String> = emptyList(),
    cast: List<TvosCastMember> = emptyList()
) = TvosMetaDetails(
    metaId = "show",
    type = type,
    name = name,
    logoUrl = null,
    backdropUrl = null,
    posterUrl = null,
    releaseLine = releaseLine,
    runtime = runtime,
    ageRating = ageRating,
    imdbRating = null,
    genres = genres,
    overview = null,
    directors = directors,
    cast = cast,
    seasons = seasons,
    primaryAction = primaryAction
)
