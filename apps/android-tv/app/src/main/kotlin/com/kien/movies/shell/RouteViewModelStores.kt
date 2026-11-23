package com.kien.movies.shell

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * One [ViewModelStore] per back stack entry, so `viewModel { DetailsViewModel(type, id) }` inside
 * a screen belongs to that entry: Details of another title gets its own ViewModel, and popping an
 * entry clears its ViewModels. Tab roots keep theirs for the life of the Activity, as tvOS keeps
 * its tab views alive.
 */
class RouteViewModelStores {
    private val owners = mutableMapOf<Long, EntryOwner>()

    fun owner(entryId: Long): ViewModelStoreOwner = owners.getOrPut(entryId) { EntryOwner() }

    /** Clears the stores of every entry not in [entryIds], tab roots excepted. */
    fun retainOnly(entryIds: Set<Long>) {
        val removed = owners.keys.filter { id -> id !in entryIds && !isTabRoot(id) }
        removed.forEach { id -> owners.remove(id)?.viewModelStore?.clear() }
    }

    fun clearAll() {
        owners.values.forEach { owner -> owner.viewModelStore.clear() }
        owners.clear()
    }

    private fun isTabRoot(entryId: Long): Boolean = RootTab.entries.any { tab -> Router.tabEntryId(tab) == entryId }

    private class EntryOwner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }
}
