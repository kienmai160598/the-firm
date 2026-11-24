package com.nuvio.tvos

import com.nuvio.app.features.addons.httpRequestRaw
import com.nuvio.app.features.trakt.TraktConfig
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The two Trakt endpoints of the device code flow. */
internal object TraktDeviceApi {
    suspend fun requestCode(): TraktDeviceCode? {
        val body = buildJsonObject { put("client_id", TraktConfig.CLIENT_ID) }
        val response = post(DEVICE_CODE_URL, body) ?: return null
        if (response.status != TRAKT_HTTP_OK) return null
        return runCatching { traktJson.decodeFromString<TraktDeviceCode>(response.body) }.getOrNull()
    }

    suspend fun poll(deviceCode: String): TraktDevicePoll {
        val body =
            buildJsonObject {
                put("code", deviceCode)
                put("client_id", TraktConfig.CLIENT_ID)
                put("client_secret", TraktConfig.CLIENT_SECRET)
            }
        val response = post(DEVICE_TOKEN_URL, body) ?: return TraktDevicePoll.Pending
        return traktDevicePoll(response.status, response.body)
    }

    /** Null when the request did not get an answer; the caller decides what that means. */
    private suspend fun post(url: String, body: JsonObject) =
        try {
            httpRequestRaw("POST", url, mapOf("Content-Type" to "application/json"), body.toString())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            null
        }
}

private const val DEVICE_CODE_URL = "https://api.trakt.tv/oauth/device/code"
private const val DEVICE_TOKEN_URL = "https://api.trakt.tv/oauth/device/token"
