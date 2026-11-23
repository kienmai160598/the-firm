package com.kien.movies.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The window behind [StartupDeadline] and [StallWatchdog], driven by a fake clock. */
class NoProgressWindowTest {
    @Test
    fun expiresWhenNoDataArrivedForTheWholeLimit() {
        val window = NoProgressWindow(limitMs = 20_000)
        window.start(nowMs = 0, loadedBytes = 1_000)

        val expired = window.isExpired(nowMs = 20_000, loadedBytes = 1_000)

        assertTrue(expired)
    }

    @Test
    fun newDataOpensAFreshWindow() {
        val window = NoProgressWindow(limitMs = 20_000)
        window.start(nowMs = 0, loadedBytes = 1_000)
        window.isExpired(nowMs = 20_000, loadedBytes = 5_000)

        val expired = window.isExpired(nowMs = 39_000, loadedBytes = 5_000)

        assertFalse(expired)
    }

    @Test
    fun aCancelledWindowNeverExpires() {
        val window = NoProgressWindow(limitMs = 20_000)
        window.start(nowMs = 0, loadedBytes = 0)
        window.cancel()

        val expired = window.isExpired(nowMs = 60_000, loadedBytes = 0)

        assertFalse(expired)
    }
}
