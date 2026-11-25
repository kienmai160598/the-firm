package com.nuvio.tvos

import com.nuvio.app.features.trailer.TRAILER_REQUEST_TIMEOUT_MS
import com.nuvio.app.features.trailer.TrailerExtractionPlatform

internal actual suspend fun readTrailerPlaylist(url: String): String? {
    val response = TrailerExtractionPlatform.performRequest(
        url = url,
        method = "GET",
        headers = TrailerExtractionPlatform.defaultHeaders,
        body = null,
        timeoutMillis = TRAILER_REQUEST_TIMEOUT_MS,
    )
    return if (response.ok) response.body else null
}
