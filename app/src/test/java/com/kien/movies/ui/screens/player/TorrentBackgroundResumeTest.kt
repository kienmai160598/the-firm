package com.kien.movies.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Test

class TorrentBackgroundResumeTest {
    @Test
    fun `resume position is where the player was when the app left the foreground`() {
        assertEquals(
            90_000L,
            torrentBackgroundResumePositionMs(
                crashRecoveryPending = false,
                crashSavedPositionMs = 0L,
                playerPositionMs = 90_000L
            )
        )
    }

    @Test
    fun `a player already released by a background decoder crash keeps the position it saved`() {
        assertEquals(
            45_000L,
            torrentBackgroundResumePositionMs(
                crashRecoveryPending = true,
                crashSavedPositionMs = 45_000L,
                playerPositionMs = null
            )
        )
    }

    @Test
    fun `no player and no saved position restarts from the beginning`() {
        assertEquals(
            0L,
            torrentBackgroundResumePositionMs(
                crashRecoveryPending = false,
                crashSavedPositionMs = 30_000L,
                playerPositionMs = null
            )
        )
        assertEquals(
            0L,
            torrentBackgroundResumePositionMs(
                crashRecoveryPending = false,
                crashSavedPositionMs = 0L,
                playerPositionMs = -5L
            )
        )
    }

    @Test
    fun `leaving again before the restarted player shows a frame keeps the pending position`() {
        assertEquals(
            244_173L,
            torrentBackgroundResumePositionMs(
                crashRecoveryPending = false,
                crashSavedPositionMs = 0L,
                playerPositionMs = 0L,
                pendingSeekPositionMs = 244_173L
            )
        )
    }

    @Test
    fun `once the seek is applied the player position is used`() {
        assertEquals(
            300_000L,
            torrentBackgroundResumePositionMs(
                crashRecoveryPending = false,
                crashSavedPositionMs = 0L,
                playerPositionMs = 300_000L,
                pendingSeekPositionMs = null
            )
        )
    }
}
