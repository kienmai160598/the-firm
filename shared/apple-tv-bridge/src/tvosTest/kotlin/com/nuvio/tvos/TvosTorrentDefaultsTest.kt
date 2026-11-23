package com.nuvio.tvos

import com.nuvio.app.features.p2p.P2pCacheSize
import com.nuvio.app.features.p2p.P2pSettingsRepository
import com.nuvio.app.features.p2p.P2pTorrentProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val READAHEAD_CAP_BYTES = 1024L * 1024L * 1024L
private const val READAHEAD_SHARE_OF_CACHE = 4

class TvosTorrentDefaultsTest {
    @Test
    fun aTorrentStartsWithTheCacheAndProfileForHighBitrateFiles() {
        P2pSettingsRepository.setCacheSize(P2pCacheSize.GB_2)
        P2pSettingsRepository.setTorrentProfile(P2pTorrentProfile.BALANCED)

        TvosTorrentDefaults.apply()

        val settings = P2pSettingsRepository.uiState.value
        assertEquals(P2pCacheSize.GB_5, settings.cacheSize)
        assertEquals(P2pTorrentProfile.FAST, settings.torrentProfile)
    }

    @Test
    fun theCacheIsLargeEnoughForTheEngineToReadAheadItsFullCap() {
        val readahead = TvosTorrentDefaults.cacheSize.bytes / READAHEAD_SHARE_OF_CACHE

        assertTrue(readahead >= READAHEAD_CAP_BYTES)
    }
}
