package com.nuvio.app.features.watched

import com.nuvio.tvos.tvosPayloadFiles

actual object WatchedStorage {
    private fun payloadKey(profileId: Int) = "watched_payload_$profileId"

    actual fun loadPayload(profileId: Int): String? =
        tvosPayloadFiles.load(payloadKey(profileId))

    actual fun savePayload(profileId: Int, payload: String) {
        tvosPayloadFiles.save(payloadKey(profileId), payload)
    }
}
