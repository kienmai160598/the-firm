package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.library.LibraryClock
import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.library.LibraryRepository
import com.nuvio.app.features.library.toLibraryItem
import com.nuvio.app.features.tracking.TrackingMembershipRemovalConfirmation
import com.nuvio.app.features.tracking.TrackingProviderId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The Library tab and the Library button on Details. `LibraryRepository` decides where a title goes:
 * the local list, or the tracker's lists (Trakt watchlist) when one is signed in and chosen as the
 * library source, which is the upstream default.
 */
object TvosLibrary {
    private val mainScope = MainScope()
    private val log = Logger.withTag("TvosLibrary")
    private val pendingRemovals = mutableMapOf<String, Set<TrackingProviderId>>()

    fun observe(onChange: (TvosLibraryState) -> Unit): TvosCancellable {
        LibraryRepository.ensureLoaded()
        val job = mainScope.launch {
            LibraryRepository.uiState
                .map(::tvosLibraryState)
                .distinctUntilChanged()
                .collect(onChange)
        }
        return TvosCancellable(job)
    }

    fun observeSaved(type: String, id: String, onChange: (Boolean) -> Unit): TvosCancellable {
        LibraryRepository.ensureLoaded()
        val job = mainScope.launch {
            LibraryRepository.uiState
                .map { LibraryRepository.isSaved(id, type) }
                .distinctUntilChanged()
                .collect(onChange)
        }
        return TvosCancellable(job)
    }

    /**
     * Adds the title shown on Details, or removes it. When removing needs a confirmation the result
     * is [TvosLibraryToggle.NEEDS_CONFIRMATION]; call again with [confirmRemoval] true to go ahead.
     */
    fun toggle(
        type: String,
        id: String,
        confirmRemoval: Boolean,
        onResult: (TvosLibraryToggle) -> Unit,
    ) {
        mainScope.launch { onResult(toggleNow(type, id, confirmRemoval)) }
    }

    private suspend fun toggleNow(type: String, id: String, confirmRemoval: Boolean): TvosLibraryToggle {
        val item = libraryItem(type, id)
        if (item == null) {
            log.w { "Toggle ignored: no details loaded for $type $id" }
            return TvosLibraryToggle.FAILED
        }
        val confirmed = if (confirmRemoval) pendingRemovals[id].orEmpty() else emptySet()
        pendingRemovals.remove(id)
        val wasSaved = LibraryRepository.isSaved(item.id, item.type)
        return try {
            val result = LibraryRepository.toggleSaved(item, confirmedRemovalProviders = confirmed)
            if (result.requiresRemovalConfirmation) {
                pendingRemovals[id] = result.requiredRemovalConfirmations
                    .mapTo(mutableSetOf(), TrackingMembershipRemovalConfirmation::providerId)
            }
            tvosLibraryToggle(wasSaved, result).also { outcome ->
                log.i { "Library toggle $type $id wasSaved=$wasSaved outcome=$outcome" }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.e(error) { "Library toggle failed for $type $id" }
            TvosLibraryToggle.FAILED
        }
    }

    private fun libraryItem(type: String, id: String): LibraryItem? {
        val meta = MetaDetailsRepository.uiState.value.meta?.takeIf { it.id == id }
        return meta?.toLibraryItem(LibraryClock.nowEpochMs()) ?: LibraryRepository.savedItem(id)
    }
}
