package com.kien.movies

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.home.HOME_ROWS_TAG
import com.kien.movies.shell.COMING_LATER
import com.kien.movies.shell.PLACEHOLDER_TAG
import com.kien.movies.shell.RootTab
import com.kien.movies.shell.tabTag
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

    /** N2: the sidebar reaches all five tabs and Select moves focus into the tab's content. */
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
            tv.awaitFocus(hasTestTag(PLACEHOLDER_TAG))
            tv.back()
            tv.assertFocusedTag(tabTag(tab))
        }
    }

    /**
     * N5: Library and Settings show a placeholder that holds focus. Then the tail of N2: Back on
     * the sidebar of another tab goes Home, Back on the Home sidebar leaves the app.
     */
    @Test
    fun n5_backExits() {
        for (tab in listOf(RootTab.Library, RootTab.Settings)) {
            tv.launch(DebugLaunch.ROUTE to "tab/${tab.name.lowercase()}")
            tv.await(hasText(COMING_LATER))
            tv.awaitFocus(hasTestTag(PLACEHOLDER_TAG))
            tv.close()
        }
        tv.launch(DebugLaunch.ROUTE to "tab/settings")
        tv.awaitFocus(hasTestTag(PLACEHOLDER_TAG), TvRobot.DEFAULT_TIMEOUT_MS)

        tv.back()
        tv.assertFocusedTag(tabTag(RootTab.Settings))
        tv.back()
        tv.assertFocusedTag(tabTag(RootTab.Home))
        tv.back()

        tv.awaitExit()
    }

    /** Back from the deepest stub (Streams) walks Details, Home content, sidebar, launcher. */
    @Test
    fun n2_backFromStreamsLeavesTheApp() {
        tv.launch(DebugLaunch.ROUTE to "details/movie/tt0032138")
        tv.await(hasText("Details"))
        tv.awaitFocus(hasTestTag(PLACEHOLDER_TAG))
        tv.select()
        tv.await(hasText("Streams"))
        tv.awaitFocus(hasTestTag(PLACEHOLDER_TAG))

        tv.back()
        tv.await(hasText("Details"))
        tv.awaitFocus(hasTestTag(PLACEHOLDER_TAG))
        tv.back()
        tv.await(hasTestTag(tabTag(RootTab.Home)))
        tv.back()
        tv.assertFocusedTag(tabTag(RootTab.Home))
        tv.back()

        tv.awaitExit()
    }

    /**
     * N4 for the shell: each entry keeps its saved state, so Back returns focus to the element that
     * opened the next screen. Uses the stub Home rows; skipped when no catalog row loads (no network).
     */
    @Test
    fun n4_backReturnsToTheSameHomeRow() {
        tv.launch()
        val rowsLoaded = runCatching { tv.await(hasTestTag(HOME_ROWS_TAG)) }.isSuccess
        assumeTrue("No Home rows loaded", rowsLoaded)
        tv.down()
        val focused = compose.onNode(isFocused()).fetchSemanticsNode()
        val rowTitle = focused.config.getOrNull(SemanticsProperties.Text)
            ?.joinToString { text -> text.text }
        assumeTrue("Second row has no title", !rowTitle.isNullOrEmpty())
        tv.select()
        val opened = runCatching { tv.await(hasText("Details"), TvRobot.FOCUS_TIMEOUT_MS) }.isSuccess
        assumeTrue("Second row is not a catalog row", opened)

        tv.back()

        tv.awaitFocus(hasText(rowTitle.orEmpty()))
    }
}
