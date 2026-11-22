package com.kien.movies.core.streams

import com.kien.movies.core.network.NetworkResult
import com.kien.movies.domain.model.AddonStreams
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Results wait for the runtime lookup (bounded by its own timeout) rather than pass through
// unranked: an autoplay caller may act on the first result, and an unmarked stream above the
// device budget is the one that stalls playback. The lookup runs beside the search, so the wait
// only shows when the search answers before the meta does.
internal fun rankedForDevice(
    searchResults: Flow<NetworkResult<List<AddonStreams>>>,
    titleDuration: suspend () -> TitleDuration
): Flow<NetworkResult<List<AddonStreams>>> = flow {
    coroutineScope {
        val title = async { titleDuration() }
        searchResults.collect { result ->
            val ranked = when (result) {
                is NetworkResult.Success -> rankedFor(result, title.await())
                else -> result
            }
            emit(ranked)
        }
        title.cancel()
    }
}

private fun rankedFor(
    result: NetworkResult.Success<List<AddonStreams>>,
    title: TitleDuration
): NetworkResult<List<AddonStreams>> {
    if (title.runtimeMinutes == null) return result

    return NetworkResult.Success(
        result.data.map { group ->
            group.copy(streams = rankP2pStreamsForDevice(group.streams, title))
        }
    )
}
