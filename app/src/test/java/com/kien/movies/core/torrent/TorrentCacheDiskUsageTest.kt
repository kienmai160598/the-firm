package com.kien.movies.core.torrent

import java.io.File
import java.nio.file.Files
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TorrentCacheDiskUsageTest {
    @Test
    fun `stored bytes add up across torrent directories`() = withCacheDirectory { cacheDirectory ->
        val firstTorrent = File(cacheDirectory, "payload/$FIRST_TORRENT_ID/Film").apply { mkdirs() }
        val secondTorrent = File(cacheDirectory, "payload/$SECOND_TORRENT_ID").apply { mkdirs() }
        File(firstTorrent, "film.mkv").writeText("12345")
        File(firstTorrent, "film.srt").writeText("12")
        File(secondTorrent, "episode.mkv").writeText("123")
        File(cacheDirectory, "outside-payload.bin").writeText("1234567")

        val storage = measureTorrentCacheStorage(cacheDirectory, ::lengthOnDisk)

        assertEquals(10L, storage.cachedBytes)
        val usableSpaceDrift = abs(cacheDirectory.usableSpace - storage.usableBytes)
        assertTrue(usableSpaceDrift <= USABLE_SPACE_DRIFT_BYTES)
    }

    @Test
    fun `sparse files count by what the sizer reports, not by their length`() =
        withCacheDirectory { cacheDirectory ->
            val torrent = File(cacheDirectory, "payload/$FIRST_TORRENT_ID").apply { mkdirs() }
            File(torrent, "film.mkv").writeText("x".repeat(4096))

            val storage = measureTorrentCacheStorage(cacheDirectory) { entry ->
                if (entry.isDirectory) StoredEntry.Directory else StoredEntry.RegularFile(512L)
            }

            assertEquals(512L, storage.cachedBytes)
        }

    @Test
    fun `missing payload directory holds nothing`() = withCacheDirectory { cacheDirectory ->
        assertEquals(0L, measureTorrentCacheStorage(cacheDirectory, ::lengthOnDisk).cachedBytes)
    }

    @Test
    fun `entries that are not real files or directories are not followed or counted`() =
        withCacheDirectory { cacheDirectory ->
            val torrent = File(cacheDirectory, "payload/$FIRST_TORRENT_ID").apply { mkdirs() }
            val linkedElsewhere = File(torrent, "linked").apply { mkdirs() }
            File(torrent, "film.mkv").writeText("12345")
            File(linkedElsewhere, "outside.bin").writeText("1234567")

            val storage = measureTorrentCacheStorage(cacheDirectory) { entry ->
                if (entry.name == "linked") StoredEntry.Skipped else lengthOnDisk(entry)
            }

            assertEquals(5L, storage.cachedBytes)
        }

    @Test
    fun `walk stops at the entry bound`() = withCacheDirectory { cacheDirectory ->
        val payload = File(cacheDirectory, "payload").apply { mkdirs() }
        repeat(5) { index -> File(payload, "part-$index.bin").writeText("12") }

        assertEquals(6L, storedTreeBytes(payload, ::lengthOnDisk, maximumEntries = 3))
        assertEquals(10L, storedTreeBytes(payload, ::lengthOnDisk, maximumEntries = 5))
    }

    @Test
    fun `title bytes come from that torrent's directory only`() =
        withCacheDirectory { cacheDirectory ->
            val current = File(cacheDirectory, "payload/$FIRST_TORRENT_ID").apply { mkdirs() }
            val other = File(cacheDirectory, "payload/$SECOND_TORRENT_ID").apply { mkdirs() }
            File(current, "film.mkv").writeText("12345")
            File(other, "episode.mkv").writeText("123")

            val uppercaseId = FIRST_TORRENT_ID.uppercase()
            val absentId = "f".repeat(40)

            assertEquals(5L, measureTorrentTitleBytes(cacheDirectory, uppercaseId, ::lengthOnDisk))
            assertEquals(0L, measureTorrentTitleBytes(cacheDirectory, absentId, ::lengthOnDisk))
        }

    @Test(expected = IllegalArgumentException::class)
    fun `a torrent id that is not a hash never becomes a path`() =
        withCacheDirectory { cacheDirectory ->
            measureTorrentTitleBytes(cacheDirectory, "../state", ::lengthOnDisk)
        }

    private fun withCacheDirectory(test: (File) -> Unit) {
        val cacheDirectory = Files.createTempDirectory("nuvio-engine-cache-").toFile()
        try {
            test(cacheDirectory)
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    private fun lengthOnDisk(entry: File): StoredEntry = when {
        entry.isDirectory -> StoredEntry.Directory
        entry.isFile -> StoredEntry.RegularFile(entry.length())
        else -> StoredEntry.Skipped
    }

    private companion object {
        const val USABLE_SPACE_DRIFT_BYTES = 256L * 1024L * 1024L
        const val FIRST_TORRENT_ID = "0123456789abcdef0123456789abcdef01234567"
        const val SECOND_TORRENT_ID = "89abcdef0123456789abcdef0123456789abcdef"
    }
}
