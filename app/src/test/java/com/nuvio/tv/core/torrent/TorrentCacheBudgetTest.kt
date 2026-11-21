package com.nuvio.tv.core.torrent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TorrentCacheBudgetTest {
    @Test
    fun `requested size is kept when the disk has room for it and the reserve`() {
        val reclaimable = TorrentCacheSize.GB_2.bytes + TORRENT_CACHE_FREE_SPACE_RESERVE_BYTES

        assertEquals(
            TorrentCacheSize.GB_2.bytes,
            fitTorrentCacheCapacity(TorrentCacheSize.GB_2.bytes, reclaimable)
        )
        assertEquals(
            TorrentCacheSize.GB_2.bytes,
            fitTorrentCacheCapacity(TorrentCacheSize.GB_2.bytes, 64L * GIBIBYTE)
        )
    }

    @Test
    fun `capacity shrinks to what the disk holds beyond the reserve`() {
        val reclaimable = 1_400L * MEBIBYTE

        assertEquals(
            reclaimable - TORRENT_CACHE_FREE_SPACE_RESERVE_BYTES,
            fitTorrentCacheCapacity(TorrentCacheSize.GB_10.bytes, reclaimable)
        )
    }

    @Test
    fun `capacity never goes negative on a nearly full disk`() {
        assertEquals(0L, fitTorrentCacheCapacity(TorrentCacheSize.GB_2.bytes, 700L * MEBIBYTE))
        assertEquals(
            0L,
            fitTorrentCacheCapacity(
                TorrentCacheSize.GB_2.bytes,
                TORRENT_CACHE_FREE_SPACE_RESERVE_BYTES
            )
        )
        assertEquals(0L, fitTorrentCacheCapacity(TorrentCacheSize.GB_2.bytes, 0L))
    }

    @Test
    fun `no persistent cache stays at zero whatever the disk holds`() {
        assertEquals(0L, fitTorrentCacheCapacity(TorrentCacheSize.NONE.bytes, 64L * GIBIBYTE))
    }

    @Test
    fun `cached torrent data counts as room for the next stream`() {
        val storage = TorrentCacheStorage(
            usableBytes = 300L * MEBIBYTE,
            cachedBytes = 500L * MEBIBYTE
        )

        assertEquals(800L * MEBIBYTE, storage.reclaimableBytes)
        assertTrue(storage.hasRoomForStream)
    }

    @Test
    fun `stream is refused below the smallest window the engine can keep`() {
        val enough = TorrentCacheStorage(
            usableBytes = TORRENT_CACHE_MINIMUM_RECLAIMABLE_BYTES,
            cachedBytes = 0L
        )
        val short = enough.copy(usableBytes = TORRENT_CACHE_MINIMUM_RECLAIMABLE_BYTES - 1L)

        assertTrue(enough.hasRoomForStream)
        assertFalse(short.hasRoomForStream)
    }

    @Test
    fun `other titles keep only what leaves the current one room to reach the capacity`() {
        val reclaimable = 1_400L * MEBIBYTE
        val capacity = fitTorrentCacheCapacity(TorrentCacheSize.GB_2.bytes, reclaimable)
        val currentTitle = 100L * MEBIBYTE

        val target = otherTitlesReclaimTargetBytes(reclaimable, capacity, currentTitle)

        val otherTitles = target - currentTitle
        val freeOnceReclaimed = reclaimable - target
        assertEquals(256L * MEBIBYTE, otherTitles)
        assertEquals(
            capacity,
            currentTitle + freeOnceReclaimed - TORRENT_ENGINE_FREE_SPACE_RESERVE_BYTES
        )
    }

    @Test
    fun `a large disk asks the engine for nothing beyond its own capacity`() {
        val reclaimable = 64L * GIBIBYTE
        val capacity = TorrentCacheSize.GB_2.bytes

        val target = otherTitlesReclaimTargetBytes(reclaimable, capacity, currentTitleBytes = 0L)

        assertTrue(target > capacity)
    }

    @Test
    fun `a nearly full disk leaves other titles nothing`() {
        val currentTitle = 40L * MEBIBYTE

        assertEquals(
            currentTitle,
            otherTitlesReclaimTargetBytes(300L * MEBIBYTE, capacityBytes = 0L, currentTitle)
        )
        assertEquals(0L, otherTitlesReclaimTargetBytes(0L, 0L, currentTitleBytes = -1L))
    }

    private companion object {
        const val MEBIBYTE = 1024L * 1024L
        const val GIBIBYTE = 1024L * MEBIBYTE
    }
}
