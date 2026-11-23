package com.kien.movies

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.home.CONTINUE_WATCHING_CARD_TAG
import com.kien.movies.home.HERO_BUTTON_TAG
import com.kien.movies.home.HOME_STATUS_TAG
import com.kien.movies.home.REMOVE_BUTTON_TAG
import com.kien.movies.support.TvRobot
import com.nuvio.tvos.TvosPlayback
import com.nuvio.tvos.TvosStream
import com.nuvio.tvos.TvosStreamTarget
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Continue Watching on Home (apps/apple-tv/UITests/ContinueWatchingTests.swift), Home side only:
 * progress is saved through the bridge before launch instead of by playing (the player is slice
 * A8). Needs the test addon: `apps/apple-tv/TestAddon/serve_test_addon.py` on the host and
 * `adb reverse tcp:8765 tcp:8765`.
 */
@RunWith(AndroidJUnit4::class)
class ContinueWatchingTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val tv = TvRobot(compose)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as MoviesApp

    @Before
    fun runtimeReady() {
        val deadline = SystemClock.uptimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)
        while (app.runtimeState.value != RuntimeState.Ready) {
            check(SystemClock.uptimeMillis() < deadline) { "TvRuntime not ready" }
            Thread.sleep(POLL_MS)
        }
    }

    @After
    fun closeApp() = tv.close()

    /** H4, Home side: select on a Continue Watching card opens Streams to resume, no question first. */
    @Test
    fun h4_continueWatchingCardOpensStreamsToResume() {
        saveProgress(MOVIE)
        launchHome()
        focusCard(MOVIE.title)

        tv.select()

        tv.await(hasText("Streams"))
        assertTrue(compose.onAllNodes(hasTestTag(CONTINUE_WATCHING_CARD_TAG)).fetchSemanticsNodes().isEmpty())
    }

    /** H5: removing a card moves focus to its neighbour, and the card stays gone after relaunch. */
    @Test
    fun h5_removedCardMovesFocusToItsNeighbourAndStaysGone() {
        saveProgress(MOVIE)
        saveProgress(EPISODE)
        launchHome()
        tv.await(card(EPISODE.title))
        focusCard(MOVIE.title)
        val neighbour = compose.onAllNodes(hasTestTag(CONTINUE_WATCHING_CARD_TAG)).fetchSemanticsNodes()
            .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString() }
            .first { title -> title != MOVIE.title }
        longPressSelect()
        tv.awaitFocus(hasTestTag(REMOVE_BUTTON_TAG))

        tv.select()

        tv.awaitFocus(card(neighbour))
        compose.waitUntil(TvRobot.FOCUS_TIMEOUT_MS) { compose.onAllNodes(card(MOVIE.title)).fetchSemanticsNodes().isEmpty() }
        tv.close()
        launchHome()
        tv.await(card(neighbour))
        assertTrue("Removed card came back", compose.onAllNodes(card(MOVIE.title)).fetchSemanticsNodes().isEmpty())
    }

    private fun card(title: String) = hasTestTag(CONTINUE_WATCHING_CARD_TAG) and hasContentDescription(title)

    private fun launchHome() {
        tv.launch(DebugLaunch.ADDON_URL to ADDON_URL)
        tv.await(hasTestTag(HERO_BUTTON_TAG) or hasTestTag(HOME_STATUS_TAG))
        tv.await(hasTestTag(CONTINUE_WATCHING_CARD_TAG))
    }

    /** From wherever Home has focus: down into the Continue Watching row, then right to [title]. */
    private fun focusCard(title: String) {
        repeat(MAX_KEYS) {
            if (compose.onAllNodes(isFocused() and hasTestTag(CONTINUE_WATCHING_CARD_TAG)).fetchSemanticsNodes().isEmpty()) tv.down()
        }
        repeat(MAX_KEYS) {
            if (compose.onAllNodes(isFocused() and card(title)).fetchSemanticsNodes().isEmpty()) tv.press(KeyEvent.KEYCODE_DPAD_RIGHT)
        }
        tv.awaitFocus(card(title))
    }

    /** A held select: the down event, a long press repeat, then the up event. */
    private fun longPressSelect() {
        val downTime = SystemClock.uptimeMillis()
        val down = KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0)
        instrumentation.sendKeySync(down)
        val held = SystemClock.uptimeMillis() + LONG_PRESS_MS
        instrumentation.sendKeySync(KeyEvent.changeTimeRepeat(down, held, 1, KeyEvent.FLAG_LONG_PRESS))
        instrumentation.sendKeySync(KeyEvent(downTime, held, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER, 0))
        compose.waitForIdle()
    }

    private fun saveProgress(target: TvosStreamTarget) {
        instrumentation.runOnMainSync {
            val saved = TvosPlayback.startSession(target, STREAM).save(
                positionMs = POSITION_MS,
                durationMs = DURATION_MS,
                isPlaying = false,
                isEnded = false,
                syncRemote = false
            )
            check(saved) { "progress save was rejected for ${target.videoId}" }
        }
    }

    private companion object {
        const val ADDON_URL = "http://127.0.0.1:8765/manifest.json"
        const val TIMEOUT_SECONDS = 60L
        const val POLL_MS = 100L
        const val LONG_PRESS_MS = 600L
        const val MAX_KEYS = 8
        const val POSITION_MS = 120_000L
        const val DURATION_MS = 600_000L

        val MOVIE = target(type = "movie", videoId = "tt1254207", parentMetaId = "tt1254207", season = null, episode = null, title = "Big Buck Bunny")
        val EPISODE = target(type = "series", videoId = "tvostest-series:1:1", parentMetaId = "tvostest-series", season = 1, episode = 1, title = "Movies tvOS Test Series")

        val STREAM = TvosStream(
            key = "home-test",
            name = "Test Pattern MP4",
            title = null,
            detail = null,
            badges = emptyList(),
            sizeLabel = null,
            kind = "url",
            url = "http://127.0.0.1:8765/media/pattern.mp4",
            externalUrl = null,
            infoHash = null,
            fileIndex = null,
            filename = null,
            trackers = emptyList(),
            addonId = "dev.nuvio.tvos.testaddon",
            addonName = "Nuvio tvOS test films",
            playableUrl = "http://127.0.0.1:8765/media/pattern.mp4",
            requestHeaders = emptyMap()
        )

        fun target(type: String, videoId: String, parentMetaId: String, season: Int?, episode: Int?, title: String) = TvosStreamTarget(
            type = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            seasonNumber = season,
            episodeNumber = episode,
            title = title,
            subtitle = null,
            backdropUrl = null,
            posterUrl = null,
            logoUrl = null,
            episodeTitle = null,
            episodeThumbnailUrl = null
        )
    }
}
