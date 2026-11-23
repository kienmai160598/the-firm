package com.kien.movies.home

import com.kien.movies.shell.AppRoute
import com.nuvio.tvos.TvosContinueWatchingRow
import com.nuvio.tvos.TvosHomeState
import com.nuvio.tvos.TvosItem
import com.nuvio.tvos.TvosRow
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenStateTest {
    @Test
    fun rowsFollowTheHomeOrder() {
        val home = homeState(
            continueWatching = TvosContinueWatchingRow("Continue Watching", emptyList()),
            upcoming = TvosContinueWatchingRow("Upcoming", emptyList()),
            rows = listOf(TvosRow("a", "Popular Movies", emptyList()))
        )

        val state = homeScreenState(home)

        assertEquals(listOf("Continue Watching", "Upcoming", "Popular Movies"), (state as HomeScreenState.Rows).rows.map(HomeRowStub::title))
    }

    @Test
    fun aCatalogRowOpensDetailsOfItsFirstItem() {
        val home = homeState(rows = listOf(TvosRow("a", "Popular Movies", listOf(item("tt1"), item("tt2")))))

        val row = (homeScreenState(home) as HomeScreenState.Rows).rows.single()

        assertEquals(AppRoute.Details(type = "movie", id = "tt1"), row.firstItem)
    }

    @Test
    fun errorWithoutRowsShowsTheMessage() {
        assertEquals(HomeScreenState.Error("No addons"), homeScreenState(homeState(errorMessage = "No addons")))
    }

    @Test
    fun noRowsAndNoErrorIsLoading() {
        assertEquals(HomeScreenState.Loading, homeScreenState(homeState(isLoading = true)))
    }

    @Test
    fun rowsWinOverAnErrorFromOneAddon() {
        val home = homeState(rows = listOf(TvosRow("a", "Popular Movies", emptyList())), errorMessage = "One catalog failed")

        assertEquals(HomeScreenState.Rows(listOf(HomeRowStub("Popular Movies", null))), homeScreenState(home))
    }

    private fun item(id: String) = TvosItem(id, "movie", id, null, null, null, null, null, null, emptyList())

    private fun homeState(
        continueWatching: TvosContinueWatchingRow? = null,
        upcoming: TvosContinueWatchingRow? = null,
        rows: List<TvosRow> = emptyList(),
        isLoading: Boolean = false,
        errorMessage: String? = null
    ) = TvosHomeState(continueWatching, upcoming, rows, isLoading, errorMessage)
}
