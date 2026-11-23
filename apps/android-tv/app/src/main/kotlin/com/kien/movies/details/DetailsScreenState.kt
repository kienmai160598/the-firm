package com.kien.movies.details

import com.nuvio.tvos.TvosDetailsState
import com.nuvio.tvos.TvosMetaDetails
import com.nuvio.tvos.TvosSeason

/** What Details shows (tvOS DetailsView.swift `content`): details win over loading, loading over error. */
sealed interface DetailsScreenState {
    data object Loading : DetailsScreenState

    data class Error(val message: String) : DetailsScreenState

    data class Loaded(val details: TvosMetaDetails, val selectedSeason: TvosSeason?) : DetailsScreenState
}

/**
 * The details to show after [state] arrives (tvOS DetailsModel.apply): a state without details
 * keeps the last loaded ones. The bridge starts every observation with `details = null`, so this
 * keeps the content (and the focused episode) when Details comes back from Streams.
 */
internal fun keptDetails(previous: TvosMetaDetails?, state: TvosDetailsState): TvosMetaDetails? = state.details ?: previous

internal fun detailsScreenState(details: TvosMetaDetails?, state: TvosDetailsState, selectedSeasonNumber: Int?): DetailsScreenState = when {
    details != null -> DetailsScreenState.Loaded(details, selectedSeason(details, selectedSeasonNumber))
    state.isLoading -> DetailsScreenState.Loading
    else -> DetailsScreenState.Error(state.errorMessage ?: DETAILS_ERROR_FALLBACK)
}

/**
 * The season the shelf shows (tvOS EpisodesSection.selectedSeason): the one the user picked, else
 * the season of the play action (Resume or Next Up), else the first. A number that matches no
 * season falls back to the first. Null only when the title has no seasons.
 */
internal fun selectedSeason(details: TvosMetaDetails, selectedNumber: Int?): TvosSeason? {
    val wanted = selectedNumber ?: preferredSeasonNumber(details)
    return details.seasons.firstOrNull { season -> season.number == wanted } ?: details.seasons.firstOrNull()
}

/** The season of the play action's target (tvOS DetailsContentView.preferredSeasonNumber). */
internal fun preferredSeasonNumber(details: TvosMetaDetails): Int? = details.primaryAction?.target?.seasonNumber

/** tvOS DetailsErrorView's text when the bridge gives no message. */
const val DETAILS_ERROR_FALLBACK = "Could not load details"
