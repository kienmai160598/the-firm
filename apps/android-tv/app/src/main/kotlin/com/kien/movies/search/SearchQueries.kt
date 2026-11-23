package com.kien.movies.search

import com.nuvio.tvos.TvosSearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The search calls of the bridge that Search makes; a seam so the rules below run on the JVM. */
internal interface SearchActions {
    fun search(query: String)

    fun clear()

    fun recordRecentSearch(query: String)

    fun removeRecentSearch(query: String)
}

/** [SearchActions] on the shared `TvosSearch` object (plain synchronous calls, see BridgeFlows). */
internal object BridgeSearchActions : SearchActions {
    override fun search(query: String) = TvosSearch.search(query)

    override fun clear() = TvosSearch.clear()

    override fun recordRecentSearch(query: String) = TvosSearch.recordRecentSearch(query)

    override fun removeRecentSearch(query: String) = TvosSearch.removeRecentSearch(query)
}

/**
 * The query rules of tvOS SearchModel.swift:
 *
 * - a query is searched [TYPING_PAUSE_MILLIS] after the last change, trimmed; a newer change
 *   cancels the older search;
 * - a blank query clears the results at once and drops a pending search;
 * - the same trimmed query again is ignored. Android adds this: the screen re-reads the field when
 *   it comes back from Details, and a second search would empty the rows and lose the focused card;
 * - opening a result records the trimmed query as a recent search; a blank query records nothing
 *   (a discover card is opened with an empty field).
 *
 * @param pause waits the typing pause; tests pass a gate they open by hand.
 */
internal class SearchQueries(
    private val scope: CoroutineScope,
    private val actions: SearchActions,
    private val pause: suspend (Long) -> Unit = { millis -> delay(millis) }
) {
    private var pendingSearch: Job? = null
    private var handledQuery: String? = null

    fun queryChanged(query: String) {
        val trimmed = query.trim()
        if (trimmed == handledQuery) return
        handledQuery = trimmed
        pendingSearch?.cancel()
        pendingSearch = null
        if (trimmed.isEmpty()) {
            actions.clear()
            return
        }
        pendingSearch = scope.launch {
            pause(TYPING_PAUSE_MILLIS)
            actions.search(trimmed)
        }
    }

    fun resultOpened(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        actions.recordRecentSearch(trimmed)
    }

    fun removeRecentSearch(query: String) = actions.removeRecentSearch(query)

    companion object {
        /** tvOS `typingPause`. */
        const val TYPING_PAUSE_MILLIS = 350L
    }
}
