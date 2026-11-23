package com.kien.movies.search

import com.nuvio.tvos.TvosRow
import com.nuvio.tvos.TvosSearchState

/** What the area under the search field shows (tvOS SearchView.swift `content`). */
internal sealed interface SearchContent {
    /** Blank query: recent searches and the discover row, either may be missing. */
    data class Start(val recentSearches: List<String>, val discover: TvosRow?) : SearchContent

    /** A query with no rows yet: a spinner while loading, else the bridge's message, if any. */
    data class Status(val isLoading: Boolean, val message: String?) : SearchContent

    data class Results(val rows: List<TvosRow>) : SearchContent

    companion object {
        val Initial: SearchContent = Start(recentSearches = emptyList(), discover = null)
    }
}

/**
 * Branches on the query first, as tvOS does, not on the bridge state: a blank field shows the start
 * view even while the bridge still holds the rows of an older query.
 *
 * Rows and items are made unique by key here, because a repeated key crashes a lazy list.
 */
internal fun searchContent(query: String, search: TvosSearchState): SearchContent = when {
    query.isBlank() -> SearchContent.Start(
        recentSearches = search.recentSearches.distinct(),
        discover = search.discover?.withUniqueItems()
    )

    search.rows.isEmpty() -> SearchContent.Status(isLoading = search.isLoading, message = search.message)

    else -> SearchContent.Results(search.rows.distinctBy(TvosRow::key).map { row -> row.withUniqueItems() })
}

private fun TvosRow.withUniqueItems(): TvosRow = copy(items = items.distinctBy { item -> item.type to item.metaId })
