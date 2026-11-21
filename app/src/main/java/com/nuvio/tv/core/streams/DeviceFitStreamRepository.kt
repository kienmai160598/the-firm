package com.nuvio.tv.core.streams

import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.data.repository.StreamRepositoryImpl
import com.nuvio.tv.domain.model.AddonStreams
import com.nuvio.tv.domain.repository.StreamRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

// Ranks P2P streams by what this device can download steadily; see rankedForDevice.
class DeviceFitStreamRepository @Inject constructor(
    private val delegate: StreamRepositoryImpl,
    private val titleRuntimeLookup: TitleRuntimeLookup
) : StreamRepository by delegate {
    override fun getStreamsFromAllAddons(
        type: String,
        videoId: String,
        season: Int?,
        episode: Int?,
        forceRefresh: Boolean,
        priorityBingeGroup: String?,
        preferredPluginOnly: Boolean,
        continuePluginOnly: Boolean
    ): Flow<NetworkResult<List<AddonStreams>>> = rankedForDevice(
        searchResults = delegate.getStreamsFromAllAddons(
            type = type,
            videoId = videoId,
            season = season,
            episode = episode,
            forceRefresh = forceRefresh,
            priorityBingeGroup = priorityBingeGroup,
            preferredPluginOnly = preferredPluginOnly,
            continuePluginOnly = continuePluginOnly
        ),
        titleDuration = { titleRuntimeLookup.titleDuration(type, videoId, season, episode) }
    )
}
