package com.kien.movies

import android.view.KeyEvent
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.player.PLAYBACK_ADVISORY_TAG
import com.kien.movies.player.PLAYER_CONTROLS_TAG
import com.kien.movies.player.PLAYER_INFO_MENU_TAG
import com.kien.movies.player.PLAYER_REMAINING_TAG
import com.kien.movies.player.PLAYER_SKIP_TAG
import com.kien.movies.player.PLAYER_SURFACE_TAG
import com.kien.movies.player.PLAYER_TRACK_MENU_TAG
import com.kien.movies.player.PlayerControl
import com.kien.movies.player.playerControlTag
import com.kien.movies.player.playerOptionTag
import com.kien.movies.support.TvRobot
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The player's ACs (docs/android-tv-rebuild.md, section 6) against the test addon
 * (apps/apple-tv/UITests/PlayerTests.swift). Needs `make test-addon` on the host and
 * `adb reverse tcp:8765 tcp:8765`.
 */
@RunWith(AndroidJUnit4::class)
class PlayerTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val tv = TvRobot(compose)

    @After
    fun closeApp() = tv.close()

    private fun playFirstStream(route: String = TEARS_OF_STEEL_STREAMS) {
        tv.launch(DebugLaunch.ROUTE to route, DebugLaunch.ADDON_URL to TEST_ADDON, DebugLaunch.OPEN_FIRST_STREAM to true)
        tv.await(hasTestTag(PLAYER_SURFACE_TAG), timeoutMillis = PLAY_TIMEOUT_MS)
        tv.assertFocusedTag(PLAYER_SURFACE_TAG)
        tv.await(hasTestTag(PLAYER_REMAINING_TAG), timeoutMillis = PLAY_TIMEOUT_MS)
    }

    /** P1, P2: the MKV plays; right twice shows 20 s on the badge; the controls hide after 5 s. */
    @Test
    fun p2_skipAddsUpAndControlsHide() {
        playFirstStream()

        tv.press(KeyEvent.KEYCODE_DPAD_RIGHT)
        tv.press(KeyEvent.KEYCODE_DPAD_RIGHT)

        tv.await(hasTestTag(PLAYER_SKIP_TAG) and hasAnyDescendant(hasText("20")))
        compose.waitUntilDoesNotExist(hasTestTag(PLAYER_CONTROLS_TAG), timeoutMillis = HIDE_TIMEOUT_MS)
    }

    /** P5: the audio menu lists the tracks and Info shows the decoder. */
    @Test
    fun p5_audioMenuAndInfoShowDecoder() {
        playFirstStream()
        pauseSoControlsStay()

        tv.press(KeyEvent.KEYCODE_DPAD_UP)
        tv.assertFocusedTag(playerControlTag(PlayerControl.Subtitles))
        tv.press(KeyEvent.KEYCODE_DPAD_RIGHT)
        tv.select()

        tv.await(hasTestTag(PLAYER_TRACK_MENU_TAG))
        tv.back()
        tv.assertFocusedTag(playerControlTag(PlayerControl.Audio))
        tv.press(KeyEvent.KEYCODE_DPAD_RIGHT)
        tv.select()
        tv.await(hasTestTag(PLAYER_INFO_MENU_TAG))
        tv.await(hasText("Decoder"))
    }

    /** P5: the subtitle menu has Off. */
    @Test
    fun p5_subtitleMenuHasOff() {
        playFirstStream()
        pauseSoControlsStay()

        tv.press(KeyEvent.KEYCODE_DPAD_UP)
        tv.select()

        tv.await(hasTestTag(playerOptionTag("off")))
    }

    /** P6: the advisory banner shows once playback starts. */
    @Test
    fun p6_advisoryShowsOnStart() {
        tv.launch(DebugLaunch.ROUTE to TEARS_OF_STEEL_STREAMS, DebugLaunch.ADDON_URL to TEST_ADDON, DebugLaunch.OPEN_FIRST_STREAM to true)

        tv.await(hasTestTag(PLAYBACK_ADVISORY_TAG), timeoutMillis = PLAY_TIMEOUT_MS)

        compose.waitUntilDoesNotExist(hasTestTag(PLAYBACK_ADVISORY_TAG), timeoutMillis = HIDE_TIMEOUT_MS)
    }

    /** P7: Back hides the controls first, then leaves the player for Streams. */
    @Test
    fun p7_backFollowsTheOrder() {
        playFirstStream()
        pauseSoControlsStay()
        tv.press(KeyEvent.KEYCODE_DPAD_UP)
        tv.select()
        tv.await(hasTestTag(PLAYER_TRACK_MENU_TAG))

        tv.back()
        tv.assertFocusedTag(playerControlTag(PlayerControl.Subtitles))
        tv.back()
        tv.assertFocusedTag(PLAYER_SURFACE_TAG)
        tv.back()
        compose.waitUntilDoesNotExist(hasTestTag(PLAYER_CONTROLS_TAG), timeoutMillis = TvRobot.FOCUS_TIMEOUT_MS)
        tv.back()

        compose.waitUntilDoesNotExist(hasTestTag(PLAYER_SURFACE_TAG), timeoutMillis = TvRobot.FOCUS_TIMEOUT_MS)
    }

    /** P2: select pauses and the bar shows the pause icon in place of the elapsed time. */
    @Test
    fun p2_selectPausesAndShowsThePauseIcon() {
        playFirstStream(route = BIG_BUCK_BUNNY_STREAMS)

        tv.select()

        tv.await(hasContentDescription("Paused"))
    }

    /** Select pauses; paused, the controls do not hide, so Up reaches the buttons. */
    private fun pauseSoControlsStay() {
        tv.select()
        tv.await(hasContentDescription("Paused"))
    }

    private companion object {
        const val TEST_ADDON = "http://127.0.0.1:8765/manifest.json"
        const val TEARS_OF_STEEL_STREAMS = "streams/movie/tt2285752"
        const val BIG_BUCK_BUNNY_STREAMS = "streams/movie/tt1254207"
        const val PLAY_TIMEOUT_MS = 60_000L
        const val HIDE_TIMEOUT_MS = 12_000L
    }
}
