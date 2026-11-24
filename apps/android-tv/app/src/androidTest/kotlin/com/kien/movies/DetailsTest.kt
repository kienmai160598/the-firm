package com.kien.movies

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.details.DETAILS_CONTENT_TAG
import com.kien.movies.details.DETAILS_ERROR_TAG
import com.kien.movies.details.DETAILS_INFO_TAG
import com.kien.movies.details.DETAILS_LOADING_TAG
import com.kien.movies.details.DETAILS_OVERVIEW_TAG
import com.kien.movies.details.DETAILS_PLAY_TAG
import com.kien.movies.details.DETAILS_RESUME_BAR_TAG
import com.kien.movies.details.DETAILS_RESUME_LABEL_TAG
import com.kien.movies.details.castTag
import com.kien.movies.details.episodeTag
import com.kien.movies.details.seasonTag
import com.kien.movies.support.TvRobot
import com.nuvio.tvos.TvosPlayback
import com.nuvio.tvos.TvosStream
import com.nuvio.tvos.TvosStreamTarget
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Details ACs D1 to D5 (docs/android-tv-rebuild.md, section 6), with the remote's keys only.
 * Breaking Bad comes from the default addon (network), as in tvOS NavigationTests; the part
 * watched series comes from the test addon (`apps/apple-tv/TestAddon`, reached through
 * `adb reverse tcp:8765 tcp:8765`), as in tvOS ContinueWatchingTests.
 */
@RunWith(AndroidJUnit4::class)
class DetailsTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val tv = TvRobot(compose)

    @After
    fun closeApp() = tv.close()

    /** D1: logo or title, overview, facts and the play label from Kotlin; Play has focus. */
    @Test
    fun d1_detailsShowsTitleOverviewAndPlayLabel() {
        tv.launch(DebugLaunch.ROUTE to BREAKING_BAD)

        tv.awaitFocus(hasTestTag(DETAILS_PLAY_TAG), TvRobot.DEFAULT_TIMEOUT_MS)

        tv.await(hasText("Breaking Bad") or hasContentDescription("Breaking Bad"))
        tv.await(hasTestTag(DETAILS_OVERVIEW_TAG))
        tv.await(hasTestTag(DETAILS_PLAY_TAG) and hasTextStarting("Play"), TvRobot.FOCUS_TIMEOUT_MS)
    }

    /** D2: a part watched episode turns Play into Resume with its bar, the episode shows its percentage. */
    @Test
    fun d2_partWatchedEpisodeShowsResumeBarAndPercentage() {
        tv.launch(DebugLaunch.ROUTE to TEST_SERIES, DebugLaunch.ADDON_URL to TEST_ADDON_URL)
        tv.awaitFocus(hasTestTag(DETAILS_PLAY_TAG), TvRobot.DEFAULT_TIMEOUT_MS)

        saveProgress(TEST_SERIES_EPISODE_1, positionMs = 120_000, durationMs = 600_000)

        tv.await(hasTestTag(DETAILS_PLAY_TAG) and hasTextStarting("Resume S1 E1"), TvRobot.FOCUS_TIMEOUT_MS)
        tv.await(hasTestTag(DETAILS_RESUME_BAR_TAG))
        tv.await(hasTestTag(DETAILS_RESUME_LABEL_TAG))
        tv.await(hasTestTag(episodeTag(TEST_SERIES_EPISODE_1.videoId)) and hasStateEnding("% watched"))
    }

    /** D3: down from Play lands on the selected season; coming back from the shelf lands on it again. */
    @Test
    fun d3_seasonPickerFocusesTheSelectedSeason() {
        tv.launch(DebugLaunch.ROUTE to BREAKING_BAD)
        tv.awaitFocus(hasTestTag(DETAILS_PLAY_TAG), TvRobot.DEFAULT_TIMEOUT_MS)
        tv.down()
        tv.assertFocusedTag(seasonTag(1))
        tv.press(KeyEvent.KEYCODE_DPAD_RIGHT)
        tv.assertFocusedTag(seasonTag(2))
        tv.select()
        tv.await(hasText(SEASON_2_EPISODE_1))
        tv.down()
        tv.press(KeyEvent.KEYCODE_DPAD_RIGHT)
        tv.press(KeyEvent.KEYCODE_DPAD_UP)

        tv.assertFocusedTag(seasonTag(2))
    }

    /** D3: Back from Streams restores the episode card and keeps the season. */
    @Test
    fun d3_backFromStreamsRestoresEpisodeAndSeason() {
        tv.launch(DebugLaunch.ROUTE to BREAKING_BAD)
        tv.awaitFocus(hasTestTag(DETAILS_PLAY_TAG), TvRobot.DEFAULT_TIMEOUT_MS)
        tv.down()
        tv.press(KeyEvent.KEYCODE_DPAD_RIGHT)
        tv.select()
        tv.await(hasText(SEASON_2_EPISODE_1))
        tv.down()
        tv.press(KeyEvent.KEYCODE_DPAD_RIGHT)
        val episode = focusedTag()
        assertTrue("Focused: $episode", episode.startsWith("episode-"))
        tv.select()
        compose.waitUntil(TvRobot.DEFAULT_TIMEOUT_MS) { compose.onAllNodes(hasTestTag(DETAILS_CONTENT_TAG)).fetchSemanticsNodes().isEmpty() }

        tv.back()

        tv.assertFocusedTag(episode)
        compose.onNodeWithTag(DETAILS_CONTENT_TAG).assertExists()
        tv.await(hasText(SEASON_2_EPISODE_1), TvRobot.FOCUS_TIMEOUT_MS)
    }

    /** D4: the D-pad scrolls down to Cast, then Information (tvOS has no About section). */
    @Test
    fun d4_castAndInformationReachableByScrolling() {
        tv.launch(DebugLaunch.ROUTE to BREAKING_BAD)
        tv.awaitFocus(hasTestTag(DETAILS_PLAY_TAG), TvRobot.DEFAULT_TIMEOUT_MS)
        // Play, the season tabs, the episodes, then Cast: a lazy page composes Cast only once near.
        tv.down()
        tv.down()
        tv.down()
        tv.assertFocusedTag(castTag(0))

        tv.down()

        tv.assertFocusedTag(DETAILS_INFO_TAG)
    }

    /** D5: while the details load, the loading state holds focus. */
    @Test
    fun d5_loadingStateHoldsFocus() {
        // A title no other test opens: one already loaded in this process skips the loading state.
        tv.launch(DebugLaunch.ROUTE to UNLOADED_SERIES)

        tv.awaitFocus(hasTestTag(DETAILS_LOADING_TAG), TvRobot.DEFAULT_TIMEOUT_MS)
    }

    /** D5: a title no addon knows ends in the error state, which holds focus. */
    @Test
    fun d5_errorStateHoldsFocus() {
        tv.launch(DebugLaunch.ROUTE to UNKNOWN_TITLE)

        tv.awaitFocus(hasTestTag(DETAILS_ERROR_TAG), TvRobot.DEFAULT_TIMEOUT_MS)
    }

    private fun focusedTag(): String = compose.onNode(isFocused()).fetchSemanticsNode().config.getOrNull(SemanticsProperties.TestTag).orEmpty()

    /** Saves progress as the player would, on the main thread where the bridge lives. */
    private fun saveProgress(target: TvosStreamTarget, positionMs: Long, durationMs: Long) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            TvosPlayback.startSession(target, TEST_STREAM)
                .save(positionMs = positionMs, durationMs = durationMs, isPlaying = false, isEnded = false, syncRemote = false)
        }
        compose.waitForIdle()
    }

    private companion object {
        const val BREAKING_BAD = "details/series/tt0903747"
        const val TEST_SERIES = "details/series/tvostest-series"
        const val UNLOADED_SERIES = "details/series/tt0141842"
        const val UNKNOWN_TITLE = "details/movie/tt0000000000"
        const val TEST_ADDON_URL = "http://127.0.0.1:8765/manifest.json"

        // The episode title under its "EPISODE 1" overline.
        const val SEASON_2_EPISODE_1 = "E1  Seven Thirty-Seven"

        val TEST_SERIES_EPISODE_1 = TvosStreamTarget(
            type = "series",
            videoId = "tvostest-series:1:1",
            parentMetaId = "tvostest-series",
            seasonNumber = 1,
            episodeNumber = 1,
            title = "Movies tvOS Test Series",
            subtitle = null,
            backdropUrl = null,
            posterUrl = null,
            logoUrl = null,
            episodeTitle = null,
            episodeThumbnailUrl = null
        )

        val TEST_STREAM = TvosStream(
            key = "details-test",
            name = "Details test",
            title = null,
            detail = null,
            badges = emptyList(),
            sizeLabel = null,
            kind = "url",
            url = "http://127.0.0.1:8765/episode.mp4",
            externalUrl = null,
            infoHash = null,
            fileIndex = null,
            filename = null,
            trackers = emptyList(),
            addonId = "tvostest",
            addonName = "Test",
            playableUrl = "http://127.0.0.1:8765/episode.mp4",
            requestHeaders = emptyMap()
        )

        fun hasContentDescription(value: String) = SemanticsMatcher("contentDescription = $value") { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(value) == true
        }

        fun hasTextStarting(prefix: String) = SemanticsMatcher("text starts with $prefix") { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text.startsWith(prefix) } == true
        }

        fun hasStateEnding(suffix: String) = SemanticsMatcher("stateDescription ends with $suffix") { node ->
            node.config.getOrNull(SemanticsProperties.StateDescription)?.endsWith(suffix) == true
        }
    }
}
