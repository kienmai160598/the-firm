package com.kien.movies.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kien.movies.bridge.BridgeFlows
import com.nuvio.tvos.TvosTrakt
import com.nuvio.tvos.TvosTraktFailure
import com.nuvio.tvos.TvosTraktPhase
import com.nuvio.tvos.TvosTraktState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** The Trakt account, kept in the shared `TvosTrakt` (tvOS TraktAccountModel.swift). */
class TraktAccountViewModel(
    initial: TvosTraktState = TvosTrakt.current(),
    states: Flow<TvosTraktState> = BridgeFlows.trakt()
) : ViewModel() {
    val state: StateFlow<TvosTraktState> = states.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initial)

    fun signIn() = TvosTrakt.startSignIn()

    fun cancel() = TvosTrakt.cancelSignIn()

    fun signOut() = TvosTrakt.signOut()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** The value the Settings list shows on the Account row. */
val TvosTraktState.summary: String
    get() = when (phase) {
        TvosTraktPhase.SIGNED_IN -> username ?: "Signed In"
        TvosTraktPhase.NOT_CONFIGURED -> NOT_AVAILABLE
        else -> "Not Signed In"
    }

/** Why the last sign in stopped, in the words the page shows under the button. */
val TvosTraktState.failureText: String?
    get() = when (failure) {
        TvosTraktFailure.EXPIRED -> "The code expired before it was approved. Sign in again for a new one."
        TvosTraktFailure.DENIED -> "Access was denied on Trakt."
        TvosTraktFailure.FAILED -> "Trakt could not be reached. Check the network and try again."
        null -> null
    }

const val NOT_AVAILABLE = "Not Available"
const val NOT_CONFIGURED_FOOTER =
    "This build has no Trakt client id. Add TRAKT_CLIENT_ID and TRAKT_CLIENT_SECRET to shared/local.properties and build again."
