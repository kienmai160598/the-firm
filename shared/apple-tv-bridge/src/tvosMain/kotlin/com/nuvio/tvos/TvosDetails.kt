package com.nuvio.tvos

import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.AddonsUiState
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.addons.hasPendingEnabledManifests
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.details.MetaDetailsUiState
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesRepository
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object TvosDetails {
    private val mainScope = MainScope()

    fun observeDetails(
        type: String,
        id: String,
        onChange: (TvosDetailsState) -> Unit,
    ): TvosCancellable {
        val job = mainScope.launch {
            onChange(TvosDetailsState(details = null, isLoading = true, errorMessage = null))
            TvosHome.awaitAddonsReadyForMeta()
            val previousMeta = MetaDetailsRepository.uiState.value.meta
            MetaDetailsRepository.load(type, id)
            WatchProgressRepository.ensureLoaded()
            WatchedRepository.ensureLoaded()
            ContinueWatchingPreferencesRepository.ensureLoaded()
            val freshDetails = MetaDetailsRepository.uiState
                .filterNot { state -> state.isStale(previousMeta, id) }
            combine(
                freshDetails,
                WatchProgressRepository.uiState,
                WatchedRepository.uiState,
                ContinueWatchingPreferencesRepository.uiState,
                ::tvosDetailsState,
            )
                .distinctUntilChanged()
                .collect(onChange)
        }
        return TvosCancellable(job)
    }
}

private fun MetaDetailsUiState.isStale(previousMeta: MetaDetails?, requestedId: String): Boolean {
    val shownMeta = meta ?: return false
    return shownMeta === previousMeta && shownMeta.id != requestedId
}
