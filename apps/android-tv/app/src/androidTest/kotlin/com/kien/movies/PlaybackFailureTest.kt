package com.kien.movies

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.player.PLAYER_ERROR_PREFIX
import com.kien.movies.player.PLAYER_REMAINING_TAG
import com.kien.movies.player.PLAYER_SURFACE_TAG
import com.kien.movies.support.TvRobot
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Streams that fail (apps/apple-tv/UITests/PlaybackFailureTests.swift), against the test addon.
 * The notice is drawn by Streams; these tests check the player hands it over.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackFailureTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val tv = TvRobot(compose)

    @After
    fun closeApp() = tv.close()

    /** P1: the codec samples play without a notice. */
    @Test
    fun p1_codecSamplesPlay() {
        tv.launch(DebugLaunch.ROUTE to "streams/movie/tvostest-codecs", DebugLaunch.ADDON_URL to TEST_ADDON, DebugLaunch.OPEN_FIRST_STREAM to true)

        tv.await(hasTestTag(PLAYER_REMAINING_TAG), timeoutMillis = FAIL_TIMEOUT_MS)

        tv.assertFocusedTag(PLAYER_SURFACE_TAG)
    }

    /** P4: a stream that stops mid play fails after 20 s and the notice reaches Streams. */
    @Test
    fun p4_dropoutFailsWithANotice() {
        tv.launch(DebugLaunch.ROUTE to "streams/movie/tvostest-dropout", DebugLaunch.ADDON_URL to TEST_ADDON, DebugLaunch.OPEN_FIRST_STREAM to true)
        tv.await(hasTestTag(PLAYER_REMAINING_TAG), timeoutMillis = FAIL_TIMEOUT_MS)

        compose.waitUntilDoesNotExist(hasTestTag(PLAYER_SURFACE_TAG), timeoutMillis = FAIL_TIMEOUT_MS)

        tv.await(hasText(PLAYER_ERROR_PREFIX, substring = true))
    }

    private companion object {
        const val TEST_ADDON = "http://127.0.0.1:8765/manifest.json"
        const val FAIL_TIMEOUT_MS = 90_000L
    }
}
