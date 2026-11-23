package com.kien.movies.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kien.movies.bridge.BridgeFlows
import com.nuvio.tvos.TvosSearchState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Search query, results, recent searches and the discover row (tvOS SearchModel.swift).
 *
 * The query lives here, not in the screen, so it survives a trip to Details and back (the Search
 * tab root keeps its ViewModel store), and the field edits it synchronously. Every change goes
 * through [SearchQueries] (350 ms pause, blank clears, recent search rules).
 */
class SearchViewModel internal constructor(
    searchStates: Flow<TvosSearchState>,
    actions: SearchActions
) : ViewModel() {
    constructor() : this(BridgeFlows.search(), BridgeSearchActions)

    /** The text of the search field. */
    val query = TextFieldState()

    private val queries = SearchQueries(viewModelScope, actions)
    private val queryText: Flow<String> = snapshotFlow { query.text.toString() }.distinctUntilChanged()

    internal val content: StateFlow<SearchContent> = combine(queryText, searchStates, ::searchContent)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchContent.Initial)

    init {
        viewModelScope.launch { queryText.collect(queries::queryChanged) }
    }

    /** Call before opening a result: records the current query as a recent search. */
    fun resultOpened() = queries.resultOpened(query.text.toString())

    /** A recent search was picked: it becomes the query, which searches it after the pause. */
    fun pickRecentSearch(recentQuery: String) = query.setTextAndPlaceCursorAtEnd(recentQuery)

    fun removeRecentSearch(recentQuery: String) = queries.removeRecentSearch(recentQuery)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
