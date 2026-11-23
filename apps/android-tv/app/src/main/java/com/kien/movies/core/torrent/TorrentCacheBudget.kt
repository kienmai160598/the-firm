package com.kien.movies.core.torrent

private const val MEBIBYTE = 1024L * 1024L

// The engine stops a stream from growing once this much is left free.
internal const val TORRENT_ENGINE_FREE_SPACE_RESERVE_BYTES = 512L * MEBIBYTE

// Capping the cache 768 MiB short of the disk means a full cache still leaves the next stream a
// 256 MiB window above the engine's reserve, and makes the engine evict older titles, which it
// only does once the cache is over its cap.
internal const val TORRENT_CACHE_FREE_SPACE_RESERVE_BYTES = 768L * MEBIBYTE

// The engine's 512 MiB free space reserve plus the smallest window it keeps for one stream
// whatever the cap: 32 MiB playback, 64 MiB read-ahead, 64 MiB behind, 16 MiB edges.
internal const val TORRENT_CACHE_MINIMUM_RECLAIMABLE_BYTES = 688L * MEBIBYTE

internal data class TorrentCacheStorage(val usableBytes: Long, val cachedBytes: Long) {
    // What the cache already holds can be evicted, so it counts as room for the next stream.
    val reclaimableBytes: Long
        get() = usableBytes.coerceAtLeast(0L) + cachedBytes.coerceAtLeast(0L)

    val hasRoomForStream: Boolean
        get() = reclaimableBytes >= TORRENT_CACHE_MINIMUM_RECLAIMABLE_BYTES
}

internal class TorrentStorageLowException(message: String) : IllegalStateException(message)

internal fun fitTorrentCacheCapacity(requestedBytes: Long, reclaimableBytes: Long): Long = (reclaimableBytes - TORRENT_CACHE_FREE_SPACE_RESERVE_BYTES)
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
    val reclaimableAboveReserve = reclaimableBytes - TORRENT_ENGINE_FREE_SPACE_RESERVE_BYTES
    val roomForOtherTitles = (reclaimableAboveReserve - capacityBytes).coerceAtLeast(0L)

    return currentTitleBytes.coerceAtLeast(0L) + roomForOtherTitles
}
