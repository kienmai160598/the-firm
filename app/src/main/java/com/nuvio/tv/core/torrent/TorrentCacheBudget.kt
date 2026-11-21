package com.nuvio.tv.core.torrent

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.File

private const val MEBIBYTE = 1024L * 1024L
private const val STAT_BLOCK_BYTES = 512L

// Same bound as the engine's own payload scan.
private const val MaximumInspectedCacheEntries = 100_000

// The engine stops a stream from growing once this much is left free.
internal const val TorrentEngineFreeSpaceReserveBytes = 512L * MEBIBYTE

// Capping the cache 768 MiB short of the disk means a full cache still leaves the next stream a
// 256 MiB window above the engine's reserve, and makes the engine evict older titles, which it
// only does once the cache is over its cap.
internal const val TorrentCacheFreeSpaceReserveBytes = 768L * MEBIBYTE

// The engine's 512 MiB free space reserve plus the smallest window it keeps for one stream
// whatever the cap: 32 MiB playback, 64 MiB read-ahead, 64 MiB behind, 16 MiB edges.
internal const val TorrentCacheMinimumReclaimableBytes = 688L * MEBIBYTE

internal data class TorrentCacheStorage(
    val usableBytes: Long,
    val cachedBytes: Long
) {
    // What the cache already holds can be evicted, so it counts as room for the next stream.
    val reclaimableBytes: Long
        get() = usableBytes.coerceAtLeast(0L) + cachedBytes.coerceAtLeast(0L)

    val hasRoomForStream: Boolean
        get() = reclaimableBytes >= TorrentCacheMinimumReclaimableBytes
}

internal sealed interface StoredEntry {
    data object Directory : StoredEntry
    data class RegularFile(val storedBytes: Long) : StoredEntry
    data object Skipped : StoredEntry
}

internal class TorrentStorageLowException(message: String) : IllegalStateException(message)

internal fun fitTorrentCacheCapacity(requestedBytes: Long, reclaimableBytes: Long): Long =
    (reclaimableBytes - TorrentCacheFreeSpaceReserveBytes)
        .coerceAtMost(requestedBytes)
        .coerceAtLeast(0L)

// What the whole cache may hold while the current title plays, so that the title can grow to the
// full capacity before the engine's free space reserve stops it. Other titles keep only the room
// the disk has beyond that.
internal fun otherTitlesReclaimTargetBytes(
    reclaimableBytes: Long,
    capacityBytes: Long,
    currentTitleBytes: Long
): Long {
    val roomForOtherTitles =
        (reclaimableBytes - TorrentEngineFreeSpaceReserveBytes - capacityBytes).coerceAtLeast(0L)

    return currentTitleBytes.coerceAtLeast(0L) + roomForOtherTitles
}

internal fun measureTorrentTitleBytes(
    cacheDirectory: File,
    torrentId: String,
    inspect: (File) -> StoredEntry = ::inspectStoredEntry
): Long = storedTreeBytes(File(cacheDirectory, "payload/${canonicalInfoHash(torrentId)}"), inspect)

internal fun measureTorrentCacheStorage(
    cacheDirectory: File,
    inspect: (File) -> StoredEntry = ::inspectStoredEntry
): TorrentCacheStorage = TorrentCacheStorage(
    usableBytes = cacheDirectory.usableSpace,
    cachedBytes = storedTreeBytes(File(cacheDirectory, "payload"), inspect)
)

// File names under the payload root come from torrents, so the walk never follows links and
// stops at the entry bound; an undercount only makes the fitted cap smaller.
internal fun storedTreeBytes(
    root: File,
    inspect: (File) -> StoredEntry,
    maximumEntries: Int = MaximumInspectedCacheEntries
): Long {
    if (inspect(root) != StoredEntry.Directory) return 0L

    val pendingDirectories = ArrayDeque(listOf(root))
    var inspectedEntries = 0
    var storedBytes = 0L
    while (pendingDirectories.isNotEmpty()) {
        val entries = pendingDirectories.removeLast().listFiles() ?: continue
        for (entry in entries) {
            if (inspectedEntries == maximumEntries) return storedBytes
            inspectedEntries += 1

            when (val inspected = inspect(entry)) {
                StoredEntry.Directory -> pendingDirectories += entry
                is StoredEntry.RegularFile -> storedBytes += inspected.storedBytes
                StoredEntry.Skipped -> Unit
            }
        }
    }
    return storedBytes
}

// Torrent payload files are sparse, so the length overstates what is on disk. This mirrors how
// the engine measures its cache: the smaller of the logical size and the allocated blocks.
private fun inspectStoredEntry(entry: File): StoredEntry = try {
    val status = Os.lstat(entry.path)
    when {
        OsConstants.S_ISDIR(status.st_mode) -> StoredEntry.Directory
        OsConstants.S_ISREG(status.st_mode) ->
            StoredEntry.RegularFile(minOf(status.st_size, status.st_blocks * STAT_BLOCK_BYTES))
        else -> StoredEntry.Skipped
    }
} catch (_: ErrnoException) {
    // The engine may evict an entry between the listing and the stat; a missing entry holds nothing.
    StoredEntry.Skipped
}
