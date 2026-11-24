package com.nuvio.tvos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `POST /oauth/device/code`: the code the user types at the verification url. */
@Serializable
internal data class TraktDeviceCode(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_url") val verificationUrl: String,
    @SerialName("expires_in") val expiresInSeconds: Int,
    @SerialName("interval") val intervalSeconds: Int,
)

/** `POST /oauth/device/token` once the user approved. */
@Serializable
internal data class TraktDeviceToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("created_at") val createdAt: Long,
)

/** What one poll of the token endpoint means for the flow. */
internal sealed interface TraktDevicePoll {
    data class Approved(val token: TraktDeviceToken) : TraktDevicePoll

    /** Keep polling: not approved yet, or a server or network error worth another try. */
    data object Pending : TraktDevicePoll

    /** Trakt asked for a longer interval. */
    data object SlowDown : TraktDevicePoll

    data class Stopped(val failure: TvosTraktFailure) : TraktDevicePoll
}

internal val traktJson = Json { ignoreUnknownKeys = true }

/**
 * Maps a token poll answer as Trakt documents it: 200 tokens, 400 pending, 404 invalid code,
 * 409 code already used, 410 expired, 418 denied, 429 slow down.
 */
internal fun traktDevicePoll(status: Int, body: String): TraktDevicePoll =
    when (status) {
        TRAKT_HTTP_OK -> approved(body)
        HTTP_PENDING -> TraktDevicePoll.Pending
        HTTP_SLOW_DOWN -> TraktDevicePoll.SlowDown
        HTTP_EXPIRED -> TraktDevicePoll.Stopped(TvosTraktFailure.EXPIRED)
        HTTP_DENIED -> TraktDevicePoll.Stopped(TvosTraktFailure.DENIED)
        in HTTP_SERVER_ERRORS -> TraktDevicePoll.Pending
        else -> TraktDevicePoll.Stopped(TvosTraktFailure.FAILED)
    }

private fun approved(body: String): TraktDevicePoll {
    val token = runCatching { traktJson.decodeFromString<TraktDeviceToken>(body) }.getOrNull()
    return token?.let(TraktDevicePoll::Approved) ?: TraktDevicePoll.Stopped(TvosTraktFailure.FAILED)
}

/** After a slow down answer the flow waits 5 s longer between polls, as RFC 8628 asks. */
internal fun traktSlowerInterval(seconds: Int): Int = seconds + TRAKT_SLOW_DOWN_STEP_SECONDS

internal const val TRAKT_HTTP_OK = 200
private const val HTTP_PENDING = 400
private const val HTTP_EXPIRED = 410
private const val HTTP_DENIED = 418
private const val HTTP_SLOW_DOWN = 429
private val HTTP_SERVER_ERRORS = 500..599
private const val TRAKT_SLOW_DOWN_STEP_SECONDS = 5
