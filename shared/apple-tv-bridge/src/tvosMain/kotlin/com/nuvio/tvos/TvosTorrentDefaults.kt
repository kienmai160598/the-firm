package com.nuvio.tvos

import com.nuvio.app.features.p2p.P2pCacheSize
import com.nuvio.app.features.p2p.P2pSettingsRepository
import com.nuvio.app.features.p2p.P2pTorrentProfile

/**
 * tvOS has no torrent settings screen, so every torrent starts with these. The engine reads ahead
 * a quarter of its disk cache, capped at 1 GiB by `engine/stream-readahead.patch`: 5 GB of cache
 * reaches that cap, about 90 s of a 4K remux. FAST allows 200 peer connections instead of 120.
 */
internal object TvosTorrentDefaults {
    val cacheSize = P2pCacheSize.GB_5
    val profile = P2pTorrentProfile.FAST

    fun apply() {
        P2pSettingsRepository.setCacheSize(cacheSize)
        P2pSettingsRepository.setTorrentProfile(profile)
    }
}
