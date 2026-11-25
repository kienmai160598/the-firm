package com.nuvio.tvos

import kotlinx.coroutines.Job

data class TvosItem(
    val metaId: String,
    val type: String,
    val name: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val logoUrl: String?,
    val overview: String?,
    val releaseInfo: String?,
    val imdbRating: String?,
    val genres: List<String>,
)

data class TvosRow(
    val key: String,
    val title: String,
    val items: List<TvosItem>,
)

data class TvosContinueWatchingItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val progressFraction: Float,
    val progressLabel: String?,
    val isNextUp: Boolean,
    val target: TvosStreamTarget,
    /** False for a Next Up episode that is not released yet. */
    val canMarkWatched: Boolean = true,
    /** The line on the card's artwork, "S1, E1 · 17m" (`ContinueWatchingCaption`); [progressLabel] stays the spoken value. */
    val caption: String = "",
)

data class TvosContinueWatchingRow(
    val title: String,
    val items: List<TvosContinueWatchingItem>,
)

data class TvosContinueWatchingRows(
    val continueWatching: TvosContinueWatchingRow?,
    val upcoming: TvosContinueWatchingRow?,
)

data class TvosHomeState(
    val continueWatching: TvosContinueWatchingRow?,
    val upcoming: TvosContinueWatchingRow?,
    val rows: List<TvosRow>,
    val isLoading: Boolean,
    val errorMessage: String?,
)

class TvosCancellable internal constructor(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}
