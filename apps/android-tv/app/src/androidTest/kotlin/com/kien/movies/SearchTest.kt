package com.kien.movies

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.search.SEARCH_DISCOVER_TAG
import com.kien.movies.search.SEARCH_FIELD_TAG
import com.kien.movies.search.SEARCH_INPUT_TAG
import com.kien.movies.search.SEARCH_REMOVE_TAG
import com.kien.movies.search.SEARCH_RESULTS_TAG
import com.kien.movies.search.recentSearchTag
import com.kien.movies.support.TvRobot
import com.nuvio.tvos.TvosSearch
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Search's ACs Q1 to Q3 (docs/android-tv-rebuild.md, section 6), from the search cases of
 * apps/apple-tv/UITests/NavigationTests.swift. Needs the network: results and the discover row
 * come from the default addon, as on tvOS.
 */
@RunWith(AndroidJUnit4::class)
class SearchTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val tv = TvRobot(compose)

    @After
    fun closeApp() = tv.close()

    /** Q1, tvOS testTypingATitleInSearchShowsItsPoster. */
    @Test
    fun q1_typingATitleShowsResultRows() {
        openSearchAndType(BREAKING_BAD)

        tv.await(resultCard(BREAKING_BAD), RESULTS_TIMEOUT_MS)
    }

    /** Q1, the Android part: Down from the field at rest reaches the first result row. */
    @Test
    fun q1_downFromTheFieldReachesTheResults() {
        openSearch()
        record(BREAKING_BAD)
        tv.down()
        tv.awaitFocus(isRecentSearch())
        compose.onNodeWithTag(recentSearchTag(BREAKING_BAD)).requestFocus()
        tv.select()
        tv.assertFocusedTag(SEARCH_FIELD_TAG)
        tv.await(resultCard(BREAKING_BAD), RESULTS_TIMEOUT_MS)

        tv.down()

        tv.awaitFocus(hasAnyAncestor(hasTestTag(SEARCH_RESULTS_TAG)))
    }

    /** Q2, tvOS testOpeningASearchResultKeepsTheQueryInRecentSearches. */
    @Test
    fun q2_openingAResultRecordsARecentSearch() {
        openSearchAndType(BREAKING_BAD)
        tv.await(resultCard(BREAKING_BAD), RESULTS_TIMEOUT_MS)
        compose.onAllNodes(resultCard(BREAKING_BAD)).onFirst().requestFocus()
        tv.select()
        compose.waitUntil(TvRobot.FOCUS_TIMEOUT_MS) {
            compose.onAllNodes(hasTestTag(SEARCH_FIELD_TAG) or hasTestTag(SEARCH_INPUT_TAG)).fetchSemanticsNodes().isEmpty()
        }
        tv.close()

        openSearch()

        tv.await(hasTestTag(recentSearchTag(BREAKING_BAD)))
    }

    /** Q2: a long press on a recent search offers Remove; afterwards it is gone and focus stays on screen. */
    @Test
    fun q2_aRecentSearchCanBeRemoved() {
        openSearch()
        record(REMOVED_QUERY)
        tv.await(hasTestTag(recentSearchTag(REMOVED_QUERY)))
        compose.onNodeWithTag(recentSearchTag(REMOVED_QUERY)).performSemanticsAction(SemanticsActions.OnLongClick)
        tv.awaitFocus(hasTestTag(SEARCH_REMOVE_TAG))

        tv.select()

        compose.waitUntil(TvRobot.FOCUS_TIMEOUT_MS) {
            compose.onAllNodes(hasTestTag(recentSearchTag(REMOVED_QUERY))).fetchSemanticsNodes().isEmpty()
        }
        tv.awaitFocus(hasTestTag(SEARCH_FIELD_TAG) or isRecentSearch())
    }

    /** Q3: a blank query shows recent searches and the discover row, focus starts on the field. */
    @Test
    fun q3_emptyQueryShowsRecentSearchesAndDiscover() {
        openSearch()

        record(BREAKING_BAD)

        tv.await(hasTestTag(recentSearchTag(BREAKING_BAD)))
        tv.await(hasTestTag(SEARCH_DISCOVER_TAG), RESULTS_TIMEOUT_MS)
        tv.assertFocusedTag(SEARCH_FIELD_TAG)
    }

    /** Launches on the Search tab; the shell's first focus lands on the field without the IME. */
    private fun openSearch() {
        tv.launch(DebugLaunch.ROUTE to "tab/search")
        tv.awaitFocus(hasTestTag(SEARCH_FIELD_TAG), TvRobot.DEFAULT_TIMEOUT_MS)
    }

    /** Select opens the text field (and the IME), then types [query] as the IME would. */
    private fun openSearchAndType(query: String) {
        openSearch()
        tv.select()
        tv.awaitFocus(hasTestTag(SEARCH_INPUT_TAG))
        compose.onNodeWithTag(SEARCH_INPUT_TAG).performTextInput(query)
    }

    /** Records [query] through the bridge, as opening a result does. */
    private fun record(query: String) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { TvosSearch.recordRecentSearch(query) }
        tv.await(hasTestTag(recentSearchTag(query)))
    }

    private fun resultCard(title: String): SemanticsMatcher = hasContentDescription(title, substring = true) and hasAnyAncestor(hasTestTag(SEARCH_RESULTS_TAG))

    private fun isRecentSearch(): SemanticsMatcher = SemanticsMatcher("is a recent search") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(recentSearchTag("")) == true
    }

    private companion object {
        const val BREAKING_BAD = "Breaking Bad"
        const val REMOVED_QUERY = "Q2 remove me"
        const val RESULTS_TIMEOUT_MS = 30_000L
    }
}
