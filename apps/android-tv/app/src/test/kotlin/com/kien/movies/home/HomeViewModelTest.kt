package com.kien.movies.home

import com.kien.movies.shell.AppRoute
import com.nuvio.tvos.TvosContinueWatchingItem
import com.nuvio.tvos.TvosContinueWatchingRow
import com.nuvio.tvos.TvosHomeState
import com.nuvio.tvos.TvosItem
import com.nuvio.tvos.TvosRow
import com.nuvio.tvos.TvosStreamTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class HomeViewModelTest {
    @Test
    fun heroShowsUpToSixItemsOfTheFirstRowThatHaveABackdrop() {
        val first = listOf(item("a", backdrop = null)) + (1..7).map { number -> item("b$number") }
        val home = homeState(rows = listOf(TvosRow("one", "Popular", first), TvosRow("two", "Other", listOf(item("z")))))

        val hero = homeUiState(home).hero

        assertEquals(listOf("b1", "b2", "b3", "b4", "b5", "b6"), hero.map(HeroItem::metaId))
    }

    @Test
    fun heroFactsAreKindReleaseAndTwoGenres() {
        val series = item("s").copy(type = "series", releaseInfo = "2008", genres = listOf("Drama", "Crime", "Thriller"))

        val hero = homeUiState(homeState(rows = listOf(TvosRow("one", "Popular", listOf(series))))).hero.single()

        assertEquals(listOf("TV Show", "2008", "Drama, Crime"), hero.facts)
    }

    @Test
    fun heroSlotFollowsLoadingContentAndError() {
        val rowWithoutBackdrops = listOf(TvosRow("one", "Popular", listOf(item("a", backdrop = null))))

        val modes = listOf(
            homeState(isLoading = true),
            homeState(errorMessage = "No addons"),
            homeState(rows = rowWithoutBackdrops, errorMessage = "One catalog failed"),
            homeState(rows = rowWithoutBackdrops, isLoading = true),
            homeState(rows = listOf(TvosRow("one", "Popular", listOf(item("a")))))
        ).map { home -> homeUiState(home).heroMode }

        assertEquals(listOf(HeroMode.Loading, HeroMode.Status, HeroMode.None, HeroMode.Loading, HeroMode.Featured), modes)
    }

    @Test
    fun aTitleListedTwiceInACatalogShowsOnce() {
        val home = homeState(rows = listOf(TvosRow("one", "Popular", listOf(item("a"), item("b"), item("a")))))

        val row = homeUiState(home).catalogRows.single()

        assertEquals(listOf("a", "b"), row.items.map(PosterItem::metaId))
    }

    @Test
    fun rowKeysNeverClashBetweenCatalogsAndWatchingRows() {
        val home = homeState(
            continueWatching = TvosContinueWatchingRow("Continue Watching", listOf(watching("k1"))),
            upcoming = TvosContinueWatchingRow("Upcoming", listOf(watching("k2"))),
            rows = listOf(TvosRow(CONTINUE_WATCHING_KEY, "Odd catalog key", listOf(item("a"))))
        )

        val state = homeUiState(home)
        val keys = state.watchingRows.map(WatchingRowState::key) + state.catalogRows.map(CatalogRowState::key)

        assertEquals(keys.distinct(), keys)
    }

    @Test
    fun aContinueWatchingCardResumesWithoutAsking() {
        val card = watching("k1")
        val home = homeState(continueWatching = TvosContinueWatchingRow("Continue Watching", listOf(card)))

        val route = homeUiState(home).watchingRows.single().cards.single().route

        assertEquals(AppRoute.Streams(target = card.target, resumesDirectly = true), route)
    }

    @Test
    fun w5_anEpisodeCardGoesToItsShowsDetailsNotTheEpisode() {
        val home = homeState(continueWatching = TvosContinueWatchingRow("Continue Watching", listOf(watching("show"))))

        val card = homeUiState(home).watchingRows.single().cards.single()

        assertEquals(AppRoute.Details(type = "series", id = "show"), card.detailsRoute)
        assertEquals("Go to Show", card.goToDetailsTitle)
    }

    @Test
    fun w5_aMovieCardGoesToTheMoviesDetails() {
        val movie = watching("tt1").copy(target = TvosStreamTarget("movie", "tt1", "tt1", null, null, "Movie", null, null, null, null, null, null))
        val home = homeState(continueWatching = TvosContinueWatchingRow("Continue Watching", listOf(movie)))

        val card = homeUiState(home).watchingRows.single().cards.single()

        assertEquals(AppRoute.Details(type = "movie", id = "tt1"), card.detailsRoute)
        assertEquals("Go to Movie", card.goToDetailsTitle)
    }

    @Test
    fun anEpisodeCardBandShowsTheEpisodeCodeAndTheTimeLeft() {
        val home = homeState(continueWatching = TvosContinueWatchingRow("Continue Watching", listOf(watching("k1"))))

        val details = homeUiState(home).watchingRows.single().cards.single().details

        assertEquals(listOf("S1, E2", "20 min left"), details)
    }

    @Test
    fun aRowThatDidNotChangeKeepsItsInstanceWhenAnotherRowChanges() {
        val popular = TvosRow("one", "Popular", listOf(item("a")))
        val before = homeUiState(homeState(rows = listOf(popular, TvosRow("two", "New", listOf(item("b"))))))

        val after = homeUiState(homeState(rows = listOf(popular, TvosRow("two", "New", listOf(item("b"), item("c"))))), before)

        assertSame(before.catalogRows[0], after.catalogRows[0])
        assertNotSame(before.catalogRows[1], after.catalogRows[1])
    }

    private fun item(id: String, backdrop: String? = "https://img/$id.jpg") = TvosItem(id, "movie", "Title $id", null, backdrop, null, null, null, null, emptyList())

    private fun watching(key: String) = TvosContinueWatchingItem(
        key = key,
        title = "Title $key",
        subtitle = "S1 E2",
        imageUrl = null,
        progressFraction = 0.4f,
        progressLabel = "20 min left",
        isNextUp = false,
        target = TvosStreamTarget("series", "$key:1:2", key, 1, 2, "Title $key", null, null, null, null, null, null)
    )

    private fun homeState(
        continueWatching: TvosContinueWatchingRow? = null,
        upcoming: TvosContinueWatchingRow? = null,
        rows: List<TvosRow> = emptyList(),
        isLoading: Boolean = false,
        errorMessage: String? = null
    ) = TvosHomeState(continueWatching, upcoming, rows, isLoading, errorMessage)
}
