package com.nuvio.tvos

import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.search.SearchEmptyStateReason
import com.nuvio.app.features.search.SearchUiState

data class TvosSearchState(
    val rows: List<TvosRow>,
    val isLoading: Boolean,
    val message: String?,
)

internal fun tvosSearchState(search: SearchUiState): TvosSearchState =
    TvosSearchState(
        rows = search.sections.map(HomeCatalogSection::toTvosRow),
        isLoading = search.isLoading,
        message = search.emptyStateReason?.let { reason -> searchMessage(reason, search) },
    )

private fun searchMessage(reason: SearchEmptyStateReason, search: SearchUiState): String =
    when (reason) {
        SearchEmptyStateReason.NoActiveAddons -> "Install an addon to search"
        SearchEmptyStateReason.NoSearchCatalogs -> "None of your addons can search"
        SearchEmptyStateReason.NoResults -> "No results"
        SearchEmptyStateReason.RequestFailed -> search.errorMessage ?: "Search failed"
    }
