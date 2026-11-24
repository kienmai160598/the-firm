package com.nuvio.app.features.p2p

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Core's P2P engine runs the old nuvio-engine AAR, which the TV app no longer links (E7). The TV
// app streams torrents through TvosTorrent and the new engine instead, so nothing calls this; it
// only satisfies core's expect, like the tvOS bridge's iosAppStore stub.
actual object P2pStreamingEngine {
    actual val state: StateFlow<P2pStreamingState> = MutableStateFlow<P2pStreamingState>(P2pStreamingState.Idle).asStateFlow()

    actual val cacheState: StateFlow<P2pCacheUiState> = MutableStateFlow(P2pCacheUiState()).asStateFlow()

    actual suspend fun startStream(request: P2pStreamRequest): String = throw P2pStreamingException("The TV app streams torrents through TvosTorrent")

    actual suspend fun clearCache(): P2pCacheClearResult = P2pCacheClearResult(reclaimedBytes = 0L, remainingBytes = 0L, protectedBytes = 0L)

    actual fun stopStream() = Unit

    actual fun shutdown() = Unit
}
