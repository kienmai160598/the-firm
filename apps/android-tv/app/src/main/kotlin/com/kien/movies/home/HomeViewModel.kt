package com.kien.movies.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kien.movies.bridge.BridgeFlows
import com.nuvio.tvos.TvosHomeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Home's state (tvOS HomeModel.swift). Stub: slice A2's row titles until slice A4. */
class HomeViewModel(homeStates: Flow<TvosHomeState> = BridgeFlows.home()) : ViewModel() {
    val state: StateFlow<HomeScreenState> = homeStates
        .map(::homeScreenState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeScreenState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
