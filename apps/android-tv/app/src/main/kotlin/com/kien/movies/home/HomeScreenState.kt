package com.kien.movies.home

import com.kien.movies.shell.AppRoute
import com.nuvio.tvos.TvosHomeState

// Slice A2's bare Home, kept as the stub until slice A4 replaces it with real rows.

/** What the stub Home shows: a loading text, an error text or one line per row. */
sealed interface HomeScreenState {
    data object Loading : HomeScreenState

    data class Error(val message: String) : HomeScreenState

    data class Rows(val rows: List<HomeRowStub>) : HomeScreenState
}

/** A row title and the Details route of its first item, if it has one. */
data class HomeRowStub(val title: String, val firstItem: AppRoute.Details?)

internal fun homeScreenState(home: TvosHomeState): HomeScreenState {
    val watching = listOfNotNull(home.continueWatching, home.upcoming).map { row ->
        HomeRowStub(row.title, firstItem = null)
    }
    val catalogs = home.rows.map { row ->
        HomeRowStub(row.title, row.items.firstOrNull()?.let { item -> AppRoute.Details(item.type, item.metaId) })
    }
    val rows = watching + catalogs
    return when {
        rows.isNotEmpty() -> HomeScreenState.Rows(rows)
        home.errorMessage != null -> HomeScreenState.Error(home.errorMessage.orEmpty())
        else -> HomeScreenState.Loading
    }
}
