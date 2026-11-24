package com.nuvio.tvos

import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.trakt.TraktAuthRepository
import com.nuvio.app.features.trakt.TraktAuthState
import com.nuvio.app.features.trakt.TraktAuthStorage
import com.nuvio.app.features.trakt.TraktAuthUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Trakt sign in for both TV apps through the device code flow: the TV shows a short code, the
 * user approves it at trakt.tv/activate on a phone or computer, and the TV polls until Trakt hands
 * over the tokens. The tokens go into core's Trakt storage, so core's scrobble, library and
 * progress sync use them as they would after the mobile app's redirect sign in.
 */
object TvosTrakt {
    private val mainScope = MainScope()
    private val flow = MutableStateFlow(TvosTraktState(TvosTraktPhase.SIGNED_OUT))
    private var signIn: Job? = null

    fun current(): TvosTraktState {
        TraktAuthRepository.ensureLoaded()
        return state(TraktAuthRepository.uiState.value, flow.value)
    }

    fun observe(onChange: (TvosTraktState) -> Unit): TvosCancellable {
        TraktAuthRepository.ensureLoaded()
        val job = mainScope.launch {
            combine(TraktAuthRepository.uiState, flow, ::state)
                .distinctUntilChanged()
                .collect(onChange)
        }
        return TvosCancellable(job)
    }

    /** Asks Trakt for a code and waits for its approval. Does nothing while one is running. */
    fun startSignIn() {
        if (!TraktAuthRepository.hasRequiredCredentials() || signIn?.isActive == true) return
        signIn = mainScope.launch { runDeviceFlow() }
    }

    fun cancelSignIn() {
        signIn?.cancel()
        flow.value = TvosTraktState(TvosTraktPhase.SIGNED_OUT)
    }

    /** Revokes the token at Trakt and forgets it on this TV. */
    fun signOut() {
        cancelSignIn()
        TraktAuthRepository.onDisconnectRequested(ProfileRepository.activeProfileId)
    }

    private fun state(auth: TraktAuthUiState, device: TvosTraktState): TvosTraktState =
        when {
            !TraktAuthRepository.hasRequiredCredentials() -> TvosTraktState(TvosTraktPhase.NOT_CONFIGURED)
            TraktAuthRepository.isAuthenticated.value ->
                TvosTraktState(TvosTraktPhase.SIGNED_IN, username = auth.username)
            else -> device
        }

    private suspend fun runDeviceFlow() {
        flow.value = TvosTraktState(TvosTraktPhase.STARTING)
        val code = TraktDeviceApi.requestCode() ?: return stop(TvosTraktFailure.FAILED)
        flow.value =
            TvosTraktState(
                phase = TvosTraktPhase.WAITING_FOR_APPROVAL,
                userCode = code.userCode,
                verificationUrl = code.verificationUrl,
            )
        when (val outcome = awaitApproval(code)) {
            is TraktDevicePoll.Approved -> save(outcome.token)
            is TraktDevicePoll.Stopped -> stop(outcome.failure)
            TraktDevicePoll.Pending, TraktDevicePoll.SlowDown -> stop(TvosTraktFailure.EXPIRED)
        }
    }

    private suspend fun awaitApproval(code: TraktDeviceCode): TraktDevicePoll {
        var intervalSeconds = code.intervalSeconds
        val deadline = TimeSource.Monotonic.markNow() + code.expiresInSeconds.seconds
        while (deadline.hasNotPassedNow()) {
            delay(intervalSeconds.seconds)
            when (val poll = TraktDeviceApi.poll(code.deviceCode)) {
                TraktDevicePoll.Pending -> Unit
                TraktDevicePoll.SlowDown -> intervalSeconds = traktSlowerInterval(intervalSeconds)
                else -> return poll
            }
        }
        return TraktDevicePoll.Stopped(TvosTraktFailure.EXPIRED)
    }

    private suspend fun save(token: TraktDeviceToken) {
        val profileId = ProfileRepository.activeProfileId
        val auth =
            TraktAuthState(
                accessToken = token.accessToken,
                refreshToken = token.refreshToken,
                tokenType = token.tokenType,
                createdAt = token.createdAt,
                expiresIn = token.expiresIn,
            )
        TraktAuthStorage.savePayload(profileId, traktJson.encodeToString(auth))
        TraktAuthRepository.onProfileChanged(profileId)
        flow.value = TvosTraktState(TvosTraktPhase.SIGNED_OUT)
        TraktAuthRepository.refreshUserSettings(profileId)
    }

    private fun stop(failure: TvosTraktFailure) {
        flow.value = TvosTraktState(TvosTraktPhase.SIGNED_OUT, failure = failure)
    }
}
