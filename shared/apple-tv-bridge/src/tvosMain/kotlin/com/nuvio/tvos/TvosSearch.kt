package com.nuvio.tvos

import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.search.SearchRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object TvosSearch {
    private val mainScope = MainScope()

    fun observe(onChange: (TvosSearchState) -> Unit): TvosCancellable {
        val job = mainScope.launch {
            SearchRepository.uiState
                .map(::tvosSearchState)
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
}
