package com.nuvio.tv.core.streams

import android.util.Log
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.repository.MetaRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "TitleRuntimeLookup"
private const val META_LOOKUP_TIMEOUT_MS = 2_000L

// Finds how long a title runs, so a stream's size can be read as a bitrate. The details screen has
// usually cached the meta already. Opened from elsewhere, the lookup waits a short while for the
// meta addon and then gives up. A lookup that fails only leaves the streams unranked: it must
// never fail the stream search it runs beside.
class TitleRuntimeLookup @Inject constructor(
    private val metaRepository: MetaRepository
) {
    suspend fun titleDuration(type: String, videoId: String, season: Int?, episode: Int?): TitleDuration {
        val runtimeMinutes = try {
            cachedOrFetchedMeta(type, metaIdOfVideo(videoId, season, episode))
                ?.let { titleRuntimeMinutes(it, season, episode) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.w(TAG, "Runtime lookup failed: ${error.message}")
            null
        }

        return TitleDuration(runtimeMinutes, isEpisode = season != null && episode != null)
    }

    private suspend fun cachedOrFetchedMeta(type: String, metaId: String): Meta? =
        metaRepository.getCachedMeta(type, metaId) ?: withTimeoutOrNull(META_LOOKUP_TIMEOUT_MS) {
            val result = metaRepository.getMetaFromAllAddons(type = type, id = metaId)
                .firstOrNull { it !is NetworkResult.Loading }
            (result as? NetworkResult.Success)?.data
        }
}
