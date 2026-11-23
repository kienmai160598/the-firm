package com.kien.movies.player

/**
 * Fails a playback that stalls for [LIMIT_MS] without new data after it played (tvOS
 * StallWatchdog.swift). Active only while the phase is Buffering, so a pause never trips it.
 * The progress kept so far stays saved.
 */
class StallWatchdog(limitMs: Long = LIMIT_MS) {
    private val window = NoProgressWindow(limitMs)

    fun watch(nowMs: Long, loadedBytes: Long) = window.start(nowMs, loadedBytes)

    fun stop() = window.cancel()

    fun hasGivenUp(nowMs: Long, loadedBytes: Long): Boolean = window.isExpired(nowMs, loadedBytes)

    companion object {
        const val LIMIT_MS = 20_000L
        const val STOPPED_SENDING_DATA = "The stream stopped sending data."
    }
}
