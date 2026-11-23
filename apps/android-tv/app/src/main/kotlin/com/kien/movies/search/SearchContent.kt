package com.kien.movies.search

import com.nuvio.tvos.TvosItem
import com.nuvio.tvos.TvosRow
import com.nuvio.tvos.TvosSearchState

/** What the area under the search field shows (tvOS SearchView.swift `content`). */
internal sealed interface SearchContent {
    /** Blank query: recent searches and the discover row, either may be missing. */
    data class Start(val recentSearches: List<String>, val discover: TvosRow?) : SearchContent

    /** A query with no rows yet: a spinner while loading, else the bridge's message, if any. */
    data class Status(val isLoading: Boolean, val message: String?) : SearchContent

    /**
     * Results: the suggestion capsules, the "Top Results" cards, then the bridge's rows as poster
     * rows. Suggestions and top results are derived from the rows (see [searchSuggestions]);
     * the bridge has no source for either.
     */
    data class Results(
        val suggestions: List<SearchSuggestion>,
        val topResults: List<TvosItem>,
        val rows: List<TvosRow>
    ) : SearchContent

    companion object {
        val Initial: SearchContent = Start(recentSearches = emptyList(), discover = null)
    }
}

/**
 * A suggested query (tvOS draws "dune: part two" with "dune" bright and the rest dim).
 * [matchStart] until [matchEnd] is the part of [text] the typed query matched.
 */
internal data class SearchSuggestion(val text: String, val matchStart: Int, val matchEnd: Int)

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

    else -> {
        val rows = search.rows.distinctBy(TvosRow::key).map { row -> row.withUniqueItems() }
        val ranked = interleave(rows)
        SearchContent.Results(
            suggestions = searchSuggestions(query, ranked),
            topResults = ranked.take(TOP_RESULTS_LIMIT),
            rows = rows
        )
    }
}

/**
 * Suggested queries: the lowercased names of the ranked results that contain the trimmed query,
 * those that start with it first, each name once, at most [SUGGESTIONS_LIMIT].
 */
internal fun searchSuggestions(query: String, ranked: List<TvosItem>): List<SearchSuggestion> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return emptyList()
    val matches = ranked.asSequence()
        .map { item -> item.name.trim().lowercase() }
        .filter { name -> name.contains(needle) }
        .distinct()
        .toList()
    val (prefixed, inner) = matches.partition { name -> name.startsWith(needle) }
    return (prefixed + inner).take(SUGGESTIONS_LIMIT).map { name ->
        val start = name.indexOf(needle)
        SearchSuggestion(text = name, matchStart = start, matchEnd = start + needle.length)
    }
}

/**
 * Lays [items] out for a horizontal grid of [rows] rows that the grid fills column by column, so
 * they read row by row: the first `columns` items across the top row, the rest across the next.
 */
internal fun <T> rowMajorForColumnGrid(items: List<T>, rows: Int): List<T> {
    if (rows <= 1 || items.size <= 1) return items
    val columns = (items.size + rows - 1) / rows
    return buildList {
        for (column in 0 until columns) {
            for (row in 0 until rows) {
                items.getOrNull(row * columns + column)?.let(::add)
            }
        }
    }
}

/**
 * The rows' items taken in turns (first of each row, then the second of each, ...), so movies and
 * shows both show near the start of "Top Results"; each title once.
 */
private fun interleave(rows: List<TvosRow>): List<TvosItem> {
    val longest = rows.maxOfOrNull { row -> row.items.size } ?: 0
    return (0 until longest)
        .flatMap { index -> rows.mapNotNull { row -> row.items.getOrNull(index) } }
        .distinctBy { item -> item.type to item.metaId }
}

private fun TvosRow.withUniqueItems(): TvosRow = copy(items = items.distinctBy { item -> item.type to item.metaId })

/** Two rows of result cards, as many as the reference shows plus the columns that scroll in. */
internal const val TOP_RESULTS_LIMIT = 12
internal const val SUGGESTIONS_LIMIT = 3
