package com.kien.movies.bridge

import com.nuvio.tvos.TvosHome
import com.nuvio.tvos.TvosHomeState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** The bridge's observe callbacks as cold flows; collection end cancels the observation. */
object BridgeFlows {
    fun home(): Flow<TvosHomeState> = callbackFlow {
        val observation = TvosHome.observeHome { state -> trySend(state) }
        awaitClose { observation.cancel() }
    }
}
