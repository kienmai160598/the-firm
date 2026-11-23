package com.kien.movies.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kien.movies.bridge.BridgeFlows
import com.kien.movies.debug.DebugLaunch
import com.nuvio.tvos.TvosDetailsState
import com.nuvio.tvos.TvosMetaDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Details of one title (tvOS DetailsModel.swift, plus the selected season that tvOS keeps as view
 * state in EpisodesSection). Lives in the route entry's store, so the selected season and the last
 * loaded details survive a trip to Streams and back (AC D3).
 *
 * @param initialSeasonNumber the season the picker starts on; `DebugLaunch.seasonNumber` in debug.
 */
class DetailsViewModel(
    val type: String,
    val id: String,
    detailsStates: Flow<TvosDetailsState> = BridgeFlows.details(type, id),
    initialSeasonNumber: Int? = DebugLaunch.seasonNumber
) : ViewModel() {
    private val selectedSeasonNumber = MutableStateFlow(initialSeasonNumber)

    // Outside the flow chain on purpose: WhileSubscribed restarts the bridge observation, which
    // begins again with `details = null`, and the content must not fall back to Loading then.
    private var lastDetails: TvosMetaDetails? = null

    val state: StateFlow<DetailsScreenState> = combine(detailsStates, selectedSeasonNumber) { detailsState, seasonNumber ->
        val details = keptDetails(lastDetails, detailsState)
        lastDetails = details
        detailsScreenState(details, detailsState, seasonNumber)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DetailsScreenState.Loading)

    fun selectSeason(number: Int) {
        selectedSeasonNumber.value = number
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
