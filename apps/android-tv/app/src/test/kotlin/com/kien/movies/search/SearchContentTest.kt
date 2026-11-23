package com.kien.movies.search

import com.nuvio.tvos.TvosItem
import com.nuvio.tvos.TvosRow
import com.nuvio.tvos.TvosSearchState
import org.junit.Assert.assertEquals
import org.junit.Test

/** Which view tvOS SearchView.swift shows for a query and a bridge state. */
class SearchContentTest {
    @Test
    fun aBlankQueryShowsRecentSearchesAndDiscoverEvenWithOldRows() {
        val discover = TvosRow("discover:top", "Popular Movies", listOf(item("tt1")))
        val search = searchState(rows = listOf(row("old")), recentSearches = listOf("Breaking Bad"), discover = discover)

        val content = searchContent("  ", search)

        assertEquals(SearchContent.Start(listOf("Breaking Bad"), discover), content)
    }

    @Test
    fun aQueryWithoutRowsWhileLoadingShowsLoading() {
        assertEquals(SearchContent.Status(isLoading = true, message = null), searchContent("Bre", searchState(isLoading = true)))
    }

    @Test
    fun aQueryWithoutRowsShowsTheBridgeMessage() {
        assertEquals(SearchContent.Status(isLoading = false, message = "No results"), searchContent("Bre", searchState(message = "No results")))
    }

    @Test
    fun rowsWinOverLoading() {
        val rows = listOf(row("movies"))

        val content = searchContent("Bre", searchState(rows = rows, isLoading = true))

        assertEquals(SearchContent.Results(rows), content)
    }

    @Test
    fun aRepeatedItemInARowIsShownOnce() {
        val search = searchState(rows = listOf(TvosRow("movies", "Movies", listOf(item("tt1"), item("tt2"), item("tt1")))))

        val content = searchContent("Bre", search) as SearchContent.Results

        assertEquals(listOf("tt1", "tt2"), content.rows.single().items.map(TvosItem::metaId))
    }

    private fun row(key: String) = TvosRow(key, key, listOf(item("tt1")))

    private fun item(id: String) = TvosItem(id, "movie", id, null, null, null, null, null, null, emptyList())

    private fun searchState(
        rows: List<TvosRow> = emptyList(),
        isLoading: Boolean = false,
        message: String? = null,
        recentSearches: List<String> = emptyList(),
        discover: TvosRow? = null
    ) = TvosSearchState(rows, isLoading, message, recentSearches, discover)
}
