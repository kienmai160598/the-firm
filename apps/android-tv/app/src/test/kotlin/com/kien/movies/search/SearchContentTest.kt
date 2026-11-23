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

        assertEquals(rows, (content as SearchContent.Results).rows)
    }

    @Test
    fun aRepeatedItemInARowIsShownOnce() {
        val search = searchState(rows = listOf(TvosRow("movies", "Movies", listOf(item("tt1"), item("tt2"), item("tt1")))))

        val content = searchContent("Bre", search) as SearchContent.Results

        assertEquals(listOf("tt1", "tt2"), content.rows.single().items.map(TvosItem::metaId))
    }

    @Test
    fun topResultsTakeTheRowsInTurnsAndShowEachTitleOnce() {
        val movies = TvosRow("movie", "Movies", listOf(item("m1"), item("m2"), item("m3")))
        val shows = TvosRow("series", "Series", listOf(item("s1", type = "series"), item("m1")))

        val content = searchContent("x", searchState(rows = listOf(movies, shows))) as SearchContent.Results

        assertEquals(listOf("m1", "s1", "m2", "m3"), content.topResults.map(TvosItem::metaId))
    }

    @Test
    fun topResultsStopAtTwelve() {
        val many = TvosRow("movie", "Movies", (1..20).map { index -> item("m$index") })

        val content = searchContent("x", searchState(rows = listOf(many))) as SearchContent.Results

        assertEquals(TOP_RESULTS_LIMIT, content.topResults.size)
    }

    @Test
    fun suggestionsAreMatchingNamesLowercasedWithPrefixMatchesFirst() {
        val ranked = listOf(named("Arrival"), named("Dune: Part Two"), named("The Dune Story"), named("Dune"), named("DUNE"))

        val suggestions = searchSuggestions(" Dune ", ranked)

        assertEquals(
            listOf(SearchSuggestion("dune: part two", 0, 4), SearchSuggestion("dune", 0, 4), SearchSuggestion("the dune story", 4, 8)),
            suggestions
        )
    }

    @Test
    fun suggestionsStopAtTheLimit() {
        val ranked = (1..6).map { index -> named("Dune $index") }

        assertEquals(SUGGESTIONS_LIMIT, searchSuggestions("dune", ranked).size)
    }

    @Test
    fun aGridFilledByColumnsReadsRowByRow() {
        assertEquals(listOf(1, 4, 2, 5, 3, 6), rowMajorForColumnGrid(listOf(1, 2, 3, 4, 5, 6), rows = 2))
        assertEquals(listOf(1, 4, 2, 5, 3), rowMajorForColumnGrid(listOf(1, 2, 3, 4, 5), rows = 2))
        assertEquals(listOf(1, 2, 3), rowMajorForColumnGrid(listOf(1, 2, 3), rows = 1))
    }

    private fun row(key: String) = TvosRow(key, key, listOf(item("tt1")))

    private fun item(id: String, type: String = "movie") = TvosItem(id, type, id, null, null, null, null, null, null, emptyList())

    private fun named(name: String) = TvosItem(name, "movie", name, null, null, null, null, null, null, emptyList())

    private fun searchState(
        rows: List<TvosRow> = emptyList(),
        isLoading: Boolean = false,
        message: String? = null,
        recentSearches: List<String> = emptyList(),
        discover: TvosRow? = null
    ) = TvosSearchState(rows, isLoading, message, recentSearches, discover)
}
