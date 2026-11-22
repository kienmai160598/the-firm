package com.nuvio.tvos

data class TvosStreamTarget(
    val type: String,
    val videoId: String,
    val parentMetaId: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val title: String,
    val subtitle: String?,
    val backdropUrl: String?,
    val posterUrl: String?,
    val logoUrl: String?,
    val episodeTitle: String?,
    val episodeThumbnailUrl: String?,
)

data class TvosPlayAction(
    val label: String,
    val target: TvosStreamTarget,
    val resume: TvosResumePoint?,
)

data class TvosCastMember(
    val name: String,
    val role: String?,
    val photoUrl: String?,
)

data class TvosEpisode(
    val videoId: String,
    val code: String,
    val title: String,
    val overview: String?,
    val thumbnailUrl: String?,
    val releaseDate: String?,
    val runtime: String?,
    val isReleased: Boolean,
    val isWatched: Boolean,
    val progressFraction: Float,
    val streamTarget: TvosStreamTarget,
)

data class TvosSeason(
    val number: Int,
    val title: String,
    val episodes: List<TvosEpisode>,
)

data class TvosMetaDetails(
    val metaId: String,
    val type: String,
    val name: String,
    val logoUrl: String?,
    val backdropUrl: String?,
    val posterUrl: String?,
    val releaseLine: String?,
    val runtime: String?,
    val ageRating: String?,
    val imdbRating: String?,
    val genres: List<String>,
    val overview: String?,
    val directors: List<String>,
    val cast: List<TvosCastMember>,
    val seasons: List<TvosSeason>,
    val primaryAction: TvosPlayAction?,
)

data class TvosDetailsState(
    val details: TvosMetaDetails?,
    val isLoading: Boolean,
    val errorMessage: String?,
)
