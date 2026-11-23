package com.kien.movies.data.simkl

import com.kien.movies.data.local.TraktSettingsDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Holds the current [SimklAnimeIdPreference] in memory so that projection functions
 * (which run synchronously on snapshot data) can access the preference without suspending.
 *
 * Listens to the DataStore flow and updates [current] whenever it changes.
 */
@Singleton
class SimklAnimeIdPreferenceHolder @Inject constructor(settingsDataStore: TraktSettingsDataStore) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        settingsDataStore.simklAnimeIdPreference
            .onEach { preference -> current = preference }
            .launchIn(scope)
    }

    companion object {
        @Volatile
        var current: SimklAnimeIdPreference = SimklAnimeIdPreference.DEFAULT
            internal set
    }
}
