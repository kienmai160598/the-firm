package com.kien.movies

import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.details.DETAILS_PLAY_TAG
import com.kien.movies.home.HERO_BUTTON_TAG
import com.kien.movies.support.TvRobot
import com.kien.movies.trailer.HERO_TRAILER_TAG
import com.kien.movies.trailer.TRAILER_PLAYING
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Trailer ACs H8 and D6 (docs/android-tv-rebuild.md, section 6), as tvOS DetailsTrailerTests.
 * Needs the network: the titles come from the default addon and the trailers from YouTube.
 */
@RunWith(AndroidJUnit4::class)
class TrailerTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val tv = TvRobot(compose)
    private val playingTrailer = hasTestTag(HERO_TRAILER_TAG) and hasStateDescription(TRAILER_PLAYING)

    @After
    fun closeApp() = tv.close()

    /** H8: the focused spotlight item turns into its trailer; down to the rows stops it. */
    @Test
    fun h8_focusedSpotlightPlaysTheTrailerAndLeavingItStopsIt() {
        tv.launch()
        tv.awaitFocus(hasTestTag(HERO_BUTTON_TAG), TvRobot.DEFAULT_TIMEOUT_MS)

        tv.await(playingTrailer, TRAILER_TIMEOUT_MS)
        tv.down()

        tv.awaitGone(hasTestTag(HERO_TRAILER_TAG), FADE_TIMEOUT_MS)
    }

    /** D6: a focused Play button plays the title's trailer; down to the sections stops it. */
    @Test
    fun d6_focusedPlayButtonPlaysTheTrailerAndScrollingDownStopsIt() {
        tv.launch(DebugLaunch.ROUTE to INCEPTION)
        tv.awaitFocus(hasTestTag(DETAILS_PLAY_TAG), TvRobot.DEFAULT_TIMEOUT_MS)

        tv.await(playingTrailer, TRAILER_TIMEOUT_MS)
        tv.down()

        tv.awaitGone(hasTestTag(HERO_TRAILER_TAG), FADE_TIMEOUT_MS)
    }

    private companion object {
        /** Inception, a Cinemeta title with a YouTube trailer (as on tvOS). */
        const val INCEPTION = "details/movie/tt1375666"

        /** The 1 s wait, the trailer lookup and the first video frame. */
        const val TRAILER_TIMEOUT_MS = 40_000L
        const val FADE_TIMEOUT_MS = 5_000L
    }
}
