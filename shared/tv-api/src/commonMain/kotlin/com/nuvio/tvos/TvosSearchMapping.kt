package com.nuvio.tvos

import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.search.DiscoverCatalogOption
import com.nuvio.app.features.search.DiscoverUiState
import com.nuvio.app.features.search.SearchEmptyStateReason
import com.nuvio.app.features.search.SearchUiState

data class TvosSearchState(
    val rows: List<TvosRow>,
    val isLoading: Boolean,
    val message: String?,
    val recentSearches: List<String>,
    val discover: TvosRow?,
)

internal fun tvosSearchState(
    search: SearchUiState,
    discover: DiscoverUiState,
    recentSearches: List<String>,
): TvosSearchState =
    TvosSearchState(
        rows = searchRows(search.sections),
        isLoading = search.isLoading,
        message = search.emptyStateReason?.let { reason -> searchMessage(reason, search) },
        recentSearches = recentSearches,
        discover = discover.toTvosRow(),
    )

internal fun searchRows(sections: List<HomeCatalogSection>): List<TvosRow> {
    val sectionsPerType = sections.groupingBy { section -> section.target.contentType }.eachCount()
    return sections.map { section ->
        val typeLabel = mediaTypeLabel(section.target.contentType)
        val isTypeShared = (sectionsPerType[section.target.contentType] ?: 0) > 1
        section.toTvosRow().copy(
            title = if (isTypeShared) "${section.addonName} $typeLabel" else typeLabel,
        )
    }
}

private fun DiscoverUiState.toTvosRow(): TvosRow? {
    val catalog = selectedCatalog ?: return null
    if (items.isEmpty()) return null
    return TvosRow(
        key = "discover:${catalog.key}",
        title = catalog.discoverTitle(),
        items = items.map(MetaPreview::toTvosItem),
    )
}

private fun DiscoverCatalogOption.discoverTitle(): String = "$catalogName ${mediaTypeLabel(type)}"

private fun mediaTypeLabel(type: String): String =
    when (type) {
        "movie" -> "Movies"
        "series" -> "Series"
        else -> type.replaceFirstChar(Char::uppercaseChar)
    }

private fun searchMessage(reason: SearchEmptyStateReason, search: SearchUiState): String =
    when (reason) {
        SearchEmptyStateReason.NoActiveAddons -> "Install an addon to search"
        SearchEmptyStateReason.NoSearchCatalogs -> "None of your addons can search"
        SearchEmptyStateReason.NoResults -> "No results"
        SearchEmptyStateReason.RequestFailed -> search.errorMessage ?: "Search failed"
    }
