package com.kien.movies

import com.nuvio.tvos.TvosContinueWatchingRow
import com.nuvio.tvos.TvosHomeState
import com.nuvio.tvos.TvosRow
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenStateTest {
    @Test
    fun rowTitlesFollowTheHomeOrder() {
        val home = homeState(
            continueWatching = TvosContinueWatchingRow("Continue Watching", emptyList()),
            upcoming = TvosContinueWatchingRow("Upcoming", emptyList()),
            rows = listOf(TvosRow("a", "Popular Movies", emptyList()))
        )

        val state = homeScreenState(home)

        assertEquals(
            HomeScreenState.Rows(listOf("Continue Watching", "Upcoming", "Popular Movies")),
            state
        )
    }

    @Test
    fun errorWithoutRowsShowsTheMessage() {
        val state = homeScreenState(homeState(errorMessage = "No addons"))

        assertEquals(HomeScreenState.Error("No addons"), state)
    }

    @Test
    fun noRowsAndNoErrorIsLoading() {
        val state = homeScreenState(homeState(isLoading = true))

        assertEquals(HomeScreenState.Loading, state)
    }

    @Test
    fun rowsWinOverAnErrorFromOneAddon() {
        val home = homeState(
            rows = listOf(TvosRow("a", "Popular Movies", emptyList())),
            errorMessage = "One catalog failed"
        )

        assertEquals(HomeScreenState.Rows(listOf("Popular Movies")), homeScreenState(home))
    }

    private fun homeState(
        continueWatching: TvosContinueWatchingRow? = null,
        upcoming: TvosContinueWatchingRow? = null,
        rows: List<TvosRow> = emptyList(),
        isLoading: Boolean = false,
        errorMessage: String? = null
    ) = TvosHomeState(continueWatching, upcoming, rows, isLoading, errorMessage)
}
