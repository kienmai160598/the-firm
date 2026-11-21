package com.nuvio.tv.core.torrent

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TorrentCacheBudgetTest {
    @Test
    fun `requested size is kept when the disk has room for it and the reserve`() {
        val reclaimable = TorrentCacheSize.GB_2.bytes + TorrentCacheFreeSpaceReserveBytes

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
            reclaimable - TorrentCacheFreeSpaceReserveBytes,
            fitTorrentCacheCapacity(TorrentCacheSize.GB_10.bytes, reclaimable)
        )
    }

    @Test
    fun `capacity never goes negative on a nearly full disk`() {
        assertEquals(0L, fitTorrentCacheCapacity(TorrentCacheSize.GB_2.bytes, 700L * MEBIBYTE))
        assertEquals(
            0L,
            fitTorrentCacheCapacity(TorrentCacheSize.GB_2.bytes, TorrentCacheFreeSpaceReserveBytes)
        )
        assertEquals(0L, fitTorrentCacheCapacity(TorrentCacheSize.GB_2.bytes, 0L))
    }

    @Test
    fun `no persistent cache stays at zero whatever the disk holds`() {
        assertEquals(0L, fitTorrentCacheCapacity(TorrentCacheSize.NONE.bytes, 64L * GIBIBYTE))
    }

    @Test
    fun `cached torrent data counts as room for the next stream`() {
        val storage = TorrentCacheStorage(usableBytes = 300L * MEBIBYTE, cachedBytes = 500L * MEBIBYTE)

        assertEquals(800L * MEBIBYTE, storage.reclaimableBytes)
        assertTrue(storage.hasRoomForStream)
    }

    @Test
    fun `stream is refused below the smallest window the engine can keep`() {
        val enough = TorrentCacheStorage(
            usableBytes = TorrentCacheMinimumReclaimableBytes,
            cachedBytes = 0L
        )
        val short = enough.copy(usableBytes = TorrentCacheMinimumReclaimableBytes - 1L)

        assertTrue(enough.hasRoomForStream)
        assertFalse(short.hasRoomForStream)
    }

    @Test
    fun `stored bytes add up across torrent directories`() {
        val cacheDirectory = Files.createTempDirectory("nuvio-engine-cache-").toFile()
        try {
            val firstTorrent = File(cacheDirectory, "payload/$FIRST_TORRENT_ID/Film").apply { mkdirs() }
            val secondTorrent = File(cacheDirectory, "payload/$SECOND_TORRENT_ID").apply { mkdirs() }
            File(firstTorrent, "film.mkv").writeText("12345")
            File(firstTorrent, "film.srt").writeText("12")
            File(secondTorrent, "episode.mkv").writeText("123")
            File(cacheDirectory, "outside-payload.bin").writeText("1234567")

            val storage = measureTorrentCacheStorage(cacheDirectory, ::lengthOnDisk)

            assertEquals(10L, storage.cachedBytes)
            assertEquals(cacheDirectory.usableSpace, storage.usableBytes, USABLE_SPACE_DRIFT_BYTES)
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    @Test
    fun `sparse files count by what the sizer reports, not by their length`() {
        val cacheDirectory = Files.createTempDirectory("nuvio-engine-cache-").toFile()
        try {
            val torrent = File(cacheDirectory, "payload/$FIRST_TORRENT_ID").apply { mkdirs() }
            File(torrent, "film.mkv").writeText("x".repeat(4096))

            val storage = measureTorrentCacheStorage(cacheDirectory) { entry ->
                if (entry.isDirectory) StoredEntry.Directory else StoredEntry.RegularFile(512L)
            }

            assertEquals(512L, storage.cachedBytes)
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    @Test
    fun `missing payload directory holds nothing`() {
        val cacheDirectory = Files.createTempDirectory("nuvio-engine-cache-").toFile()
        try {
            assertEquals(0L, measureTorrentCacheStorage(cacheDirectory, ::lengthOnDisk).cachedBytes)
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    @Test
    fun `entries that are not real files or directories are not followed or counted`() {
        val cacheDirectory = Files.createTempDirectory("nuvio-engine-cache-").toFile()
        try {
            val torrent = File(cacheDirectory, "payload/$FIRST_TORRENT_ID").apply { mkdirs() }
            val linkedElsewhere = File(torrent, "linked").apply { mkdirs() }
            File(torrent, "film.mkv").writeText("12345")
            File(linkedElsewhere, "outside.bin").writeText("1234567")

            val storage = measureTorrentCacheStorage(cacheDirectory) { entry ->
                if (entry.name == "linked") StoredEntry.Skipped else lengthOnDisk(entry)
            }

            assertEquals(5L, storage.cachedBytes)
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    @Test
    fun `walk stops at the entry bound`() {
        val cacheDirectory = Files.createTempDirectory("nuvio-engine-cache-").toFile()
        try {
            val torrent = File(cacheDirectory, "payload").apply { mkdirs() }
            repeat(5) { index -> File(torrent, "part-$index.bin").writeText("12") }

            assertEquals(6L, storedTreeBytes(torrent, ::lengthOnDisk, maximumEntries = 3))
            assertEquals(10L, storedTreeBytes(torrent, ::lengthOnDisk, maximumEntries = 5))
        } finally {
            cacheDirectory.deleteRecursively()
        }
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
            currentTitle + freeOnceReclaimed - TorrentEngineFreeSpaceReserveBytes
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

    @Test
    fun `title bytes come from that torrent's directory only`() {
        val cacheDirectory = Files.createTempDirectory("nuvio-engine-cache-").toFile()
        try {
            val current = File(cacheDirectory, "payload/$FIRST_TORRENT_ID").apply { mkdirs() }
            val other = File(cacheDirectory, "payload/$SECOND_TORRENT_ID").apply { mkdirs() }
            File(current, "film.mkv").writeText("12345")
            File(other, "episode.mkv").writeText("123")

            assertEquals(
                5L,
                measureTorrentTitleBytes(cacheDirectory, FIRST_TORRENT_ID.uppercase(), ::lengthOnDisk)
            )
            assertEquals(
                0L,
                measureTorrentTitleBytes(cacheDirectory, "f".repeat(40), ::lengthOnDisk)
            )
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a torrent id that is not a hash never becomes a path`() {
        val cacheDirectory = Files.createTempDirectory("nuvio-engine-cache-").toFile()
        try {
            measureTorrentTitleBytes(cacheDirectory, "../state", ::lengthOnDisk)
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    private fun lengthOnDisk(entry: File): StoredEntry = when {
        entry.isDirectory -> StoredEntry.Directory
        entry.isFile -> StoredEntry.RegularFile(entry.length())
        else -> StoredEntry.Skipped
    }

    private fun assertEquals(expected: Long, actual: Long, tolerance: Long) {
        assertTrue(
            "expected $actual within $tolerance of $expected",
            kotlin.math.abs(expected - actual) <= tolerance
        )
    }

    private companion object {
        const val MEBIBYTE = 1024L * 1024L
        const val GIBIBYTE = 1024L * MEBIBYTE
        const val USABLE_SPACE_DRIFT_BYTES = 256L * MEBIBYTE
        const val FIRST_TORRENT_ID = "0123456789abcdef0123456789abcdef01234567"
        const val SECOND_TORRENT_ID = "89abcdef0123456789abcdef0123456789abcdef"
    }
}
