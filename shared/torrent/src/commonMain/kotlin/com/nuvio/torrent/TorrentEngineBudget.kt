package com.nuvio.torrent

private const val MEBIBYTE = 1024L * 1024L

/** Most of the device's free memory the engine may hold: a quarter. */
private const val AVAILABLE_SHARE = 4L

/**
 * Largest store cap [budgetFor] picks, so the engine leaves the player and the rest of the app
 * their share on a 1.9 GB box however much is free when it opens.
 */
const val MAX_STORE_CAP_BYTES: Long = 192 * MEBIBYTE

/**
 * Smallest store cap [budgetFor] picks, however little memory is free. Below it, 32 MiB ahead is
 * about 4 s of a 4K remux (60 Mbit/s), too little to ride out a slow patch in the swarm, and a
 * torrent with 16 MiB pieces would fit only four of them.
 */
const val MIN_STORE_CAP_BYTES: Long = 64 * MEBIBYTE

/** Kept behind the playhead for small seeks back. */
const val BUDGET_BEHIND_BYTES: Long = 16 * MEBIBYTE

/**
 * The engine's memory budget (C ABI `movies_engine_config`). The engine checks it: every value
 * positive, ahead plus behind below the cap, the eviction threshold below the cap.
 */
data class TorrentEngineBudget(
    /** The store never holds more payload than this. */
    val storeCapBytes: Long,
    /** Downloaded ahead of each reader. */
    val aheadBytes: Long,
    /** Kept behind each reader. */
    val behindBytes: Long,
    /** Pieces no reader needs are dropped once the store holds more than this. */
    val evictionThresholdBytes: Long
)

/**
 * The budget for a device with [availableBytes] of free memory, measured when the engine opens:
 * a cap of a quarter of it, between [MIN_STORE_CAP_BYTES] and [MAX_STORE_CAP_BYTES]; half the cap ahead
 * of the reader, [BUDGET_BEHIND_BYTES] behind it, and eviction past three quarters of the cap.
 */
fun budgetFor(availableBytes: Long): TorrentEngineBudget {
    val cap = (availableBytes.coerceAtLeast(0) / AVAILABLE_SHARE)
        .coerceIn(MIN_STORE_CAP_BYTES, MAX_STORE_CAP_BYTES)
    return TorrentEngineBudget(
        storeCapBytes = cap,
        aheadBytes = cap / 2,
        behindBytes = BUDGET_BEHIND_BYTES,
        evictionThresholdBytes = cap * 3 / 4
    )
}
