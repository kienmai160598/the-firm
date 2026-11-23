package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.catalog.fetchCatalogPage
import com.nuvio.app.features.home.MetaPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Same length as the Home preview rows loaded by HomeRepository. */
private const val NEW_RELEASES_ITEM_LIMIT = 18

/** New rows for Home, loaded again whenever the installed addons change. */
internal object TvosNewReleases {
    private val log = Logger.withTag("TvosNewReleases")
    private val rowsState = MutableStateFlow<List<NewReleaseRow>>(emptyList())

    val rows: StateFlow<List<NewReleaseRow>> = rowsState

    suspend fun refreshOnAddonChanges() {
        AddonRepository.uiState
            .map { state -> newReleaseCatalogs(state.addons) }
            .distinctUntilChanged()
            .collectLatest { catalogs -> rowsState.value = load(catalogs) }
    }

    private suspend fun load(catalogs: List<NewReleaseCatalog>): List<NewReleaseRow> =
        coroutineScope {
            catalogs.map { catalog -> async { loadRow(catalog) } }.awaitAll().filterNotNull()
        }

    private suspend fun loadRow(catalog: NewReleaseCatalog): NewReleaseRow? =
        try {
            val page =
                fetchCatalogPage(
                    manifestUrl = catalog.manifestUrl,
                    type = catalog.type,
                    catalogId = catalog.catalogId,
                    genre = catalog.year,
                    maxItems = NEW_RELEASES_ITEM_LIMIT,
                )
            page.items.takeIf { it.isNotEmpty() }?.let { items ->
                NewReleaseRow(
                    groupPrefix = catalog.groupPrefix,
                    row = TvosRow(catalog.key, catalog.title, items.map(MetaPreview::toTvosItem)),
                )
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            log.e(error) { "New row ${catalog.key} for ${catalog.year} failed" }
            null
        }
}
