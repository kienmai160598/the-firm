package com.kien.movies.core.torrent

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.File

private const val STAT_BLOCK_BYTES = 512L

// Same bound as the engine's own payload scan.
private const val MAXIMUM_INSPECTED_CACHE_ENTRIES = 100_000

internal sealed interface StoredEntry {
    data object Directory : StoredEntry
    data class RegularFile(val storedBytes: Long) : StoredEntry
    data object Skipped : StoredEntry
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
    maximumEntries: Int = MAXIMUM_INSPECTED_CACHE_ENTRIES
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
    // The engine may evict an entry between listing and stat; a missing entry holds nothing.
    StoredEntry.Skipped
}
