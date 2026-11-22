package com.nuvio.tvos

import kotlinx.coroutines.Job

data class TvosItem(
    val metaId: String,
    val type: String,
    val name: String,
    val posterUrl: String?,
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
