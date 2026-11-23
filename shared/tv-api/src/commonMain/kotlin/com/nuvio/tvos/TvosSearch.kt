package com.nuvio.tvos

import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.home.buildAddonCatalogRefreshSignature
import com.nuvio.app.features.search.SearchHistoryRepository
import com.nuvio.app.features.search.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object TvosSearch {
    private val mainScope = MainScope()
    private var discoverRefresh: Job? = null

    fun observe(onChange: (TvosSearchState) -> Unit): TvosCancellable {
        SearchHistoryRepository.ensureLoaded()
        refreshDiscoverOnAddonChanges()
        val job = mainScope.launch {
            combine(
                SearchRepository.uiState,
                SearchRepository.discoverUiState,
                SearchHistoryRepository.uiState,
                ::tvosSearchState,
            )
                .distinctUntilChanged()
                .collect(onChange)
        }
        return TvosCancellable(job)
    }

    fun search(query: String) {
        SearchRepository.search(query, AddonRepository.uiState.value.addons)
    }

    fun clear() {
        SearchRepository.clear()
    }

    fun recordRecentSearch(query: String) {
        SearchHistoryRepository.recordSearch(query)
    }

    fun removeRecentSearch(query: String) {
        SearchHistoryRepository.removeSearch(query)
    }

    private fun refreshDiscoverOnAddonChanges() {
        if (discoverRefresh?.isActive == true) return
        discoverRefresh = mainScope.launch {
            AddonRepository.uiState
                .map { state -> buildAddonCatalogRefreshSignature(state.addons) }
                .distinctUntilChanged()
                .collect { SearchRepository.refreshDiscover(AddonRepository.uiState.value.addons) }
        }
    }
}
