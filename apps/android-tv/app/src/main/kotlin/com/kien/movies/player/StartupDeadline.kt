package com.kien.movies.player

/**
 * Fails a playback that receives no data for [LIMIT_MS] before it plays (tvOS
 * StartupDeadline.swift): every 20 s it compares the bytes loaded with the start of the window;
 * more data opens a new window, none expires. Active only while the phase is Loading.
 * Pure: the controller passes the clock and the byte count on every tick.
 */
class StartupDeadline(limitMs: Long = LIMIT_MS) {
    private val window = NoProgressWindow(limitMs)

    fun start(nowMs: Long, loadedBytes: Long) = window.start(nowMs, loadedBytes)

    fun cancel() = window.cancel()

    fun isExpired(nowMs: Long, loadedBytes: Long): Boolean = window.isExpired(nowMs, loadedBytes)

    companion object {
        const val LIMIT_MS = 20_000L
        const val NO_DATA = "The stream sent no data for 20 seconds before it started playing."
    }
}

/**
 * A window of [limitMs] that expires when no new data arrived during it. [start] is a no op
 * while a window is open; [cancel] closes it.
 */
class NoProgressWindow(private val limitMs: Long) {
    private var startMs: Long? = null
    private var startBytes = 0L

    val isOpen: Boolean get() = startMs != null

    fun start(nowMs: Long, loadedBytes: Long) {
        if (startMs != null) return
        startMs = nowMs
        startBytes = loadedBytes
    }

    fun cancel() {
        startMs = null
    }

    fun isExpired(nowMs: Long, loadedBytes: Long): Boolean {
        val opened = startMs ?: return false
        if (nowMs - opened < limitMs) return false
        if (loadedBytes > startBytes) {
            startMs = nowMs
            startBytes = loadedBytes
            return false
        }
        startMs = null
        return true
    }
}
