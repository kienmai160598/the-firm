package com.nuvio.app.features.watchprogress

import com.nuvio.tvos.tvosPayloadFiles

internal actual object WatchProgressStorage {
    private fun payloadKey(profileId: Int) = "watch_progress_payload_$profileId"

    actual fun loadPayload(profileId: Int): String? =
        tvosPayloadFiles.load(payloadKey(profileId))

    actual fun savePayload(profileId: Int, payload: String) {
        tvosPayloadFiles.save(payloadKey(profileId), payload)
    }
}
