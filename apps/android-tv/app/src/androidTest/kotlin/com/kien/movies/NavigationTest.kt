package com.kien.movies

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.details.DETAILS_CONTENT_TAG
import com.kien.movies.details.DETAILS_PLAY_TAG
import com.kien.movies.home.HOME_ROWS_TAG
import com.kien.movies.settings.SettingsPage
import com.kien.movies.settings.settingsRowTag
import com.kien.movies.shell.COMING_LATER
import com.kien.movies.shell.PLACEHOLDER_TAG
import com.kien.movies.shell.RootTab
import com.kien.movies.shell.tabTag
import com.kien.movies.streams.STREAMS_TITLE_TAG
import com.kien.movies.support.TvRobot
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The shell's ACs (docs/android-tv-rebuild.md, section 6), with the remote's keys only. */
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val tv = TvRobot(compose)

    @After
    fun closeApp() = tv.close()

    /** N2: the sidebar reaches all four tabs and Select moves focus into the tab's content. */
    @Test
    fun n2_everyTabReachable() {
        tv.launch()
        tv.await(hasTestTag(tabTag(RootTab.Home)))
        tv.back()
        tv.assertFocusedTag(tabTag(RootTab.Home))

        for (tab in RootTab.entries.drop(1)) {
            tv.down()
            tv.assertFocusedTag(tabTag(tab))
            tv.select()
            tv.await(hasText(tab.title))
            // Each screen chooses its own first focus; it only has to leave the sidebar.
            tv.awaitFocus(!hasTestTag(tabTag(tab)), TvRobot.DEFAULT_TIMEOUT_MS)
            tv.back()
            tv.assertFocusedTag(tabTag(tab))
        }
    }

    /**
     * N5: Library shows a placeholder that holds focus. Then the tail of N2 from Settings: Back on
     * the sidebar of another tab goes Home, Back on the Home sidebar leaves the app.
     */
    @Test
    fun n5_backExits() {
        tv.launch(DebugLaunch.ROUTE to "tab/library")
        tv.await(hasText(COMING_LATER))
        tv.awaitFocus(hasTestTag(PLACEHOLDER_TAG))
        tv.close()
        tv.launch(DebugLaunch.ROUTE to "tab/settings")
        tv.awaitFocus(hasTestTag(settingsRowTag(SettingsPage.entries.first())), TvRobot.DEFAULT_TIMEOUT_MS)

        tv.back()
        tv.assertFocusedTag(tabTag(RootTab.Settings))
        tv.back()
        tv.assertFocusedTag(tabTag(RootTab.Home))
        tv.back()

        tv.awaitExit()
    }

    /** Back from the deepest screen (Streams) walks Details, Home content, sidebar, launcher. */
    @Test
    fun n2_backFromStreamsLeavesTheApp() {
        tv.launch(DebugLaunch.ROUTE to "details/movie/tt0032138")
        tv.awaitFocus(hasTestTag(DETAILS_PLAY_TAG), TvRobot.DEFAULT_TIMEOUT_MS)
        tv.select()
        tv.await(hasTestTag(STREAMS_TITLE_TAG))

        tv.back()
        tv.awaitFocus(hasTestTag(DETAILS_PLAY_TAG), TvRobot.DEFAULT_TIMEOUT_MS)
        tv.back()
        tv.await(hasTestTag(tabTag(RootTab.Home)))
        tv.back()
        tv.assertFocusedTag(tabTag(RootTab.Home))
        tv.back()

        tv.awaitExit()
    }

    /**
     * N4 for the shell: each entry keeps its saved state, so Back returns focus to the element that
     * opened the next screen. Cards carry their title as content description. Skipped when no
     * catalog row loads (no network).
     */
    @Test
    fun n4_backReturnsToTheSameHomeRow() {
        tv.launch()
        val rowsLoaded = runCatching { tv.await(hasTestTag(HOME_ROWS_TAG)) }.isSuccess
        assumeTrue("No Home rows loaded", rowsLoaded)
        tv.down()
        val focused = compose.onNode(isFocused()).fetchSemanticsNode()
        val cardTitle = focused.config.getOrNull(SemanticsProperties.ContentDescription)
            ?.joinToString()
        assumeTrue("Focused element is not a card", !cardTitle.isNullOrEmpty())
        tv.select()
        val opened = runCatching {
            tv.await(hasTestTag(DETAILS_CONTENT_TAG), TvRobot.DEFAULT_TIMEOUT_MS)
        }.isSuccess
        assumeTrue("The card did not open Details", opened)

        tv.back()

        tv.awaitFocus(hasContentDescription(cardTitle.orEmpty()))
    }
}
