package com.nuvio.app.features.library

import com.nuvio.tvos.tvosPayloadFiles

internal actual object LibraryStorage {
    private fun payloadKey(profileId: Int) = "library_payload_$profileId"

    actual fun loadPayload(profileId: Int): String? =
        tvosPayloadFiles.load(payloadKey(profileId))

    actual fun savePayload(profileId: Int, payload: String) {
        tvosPayloadFiles.save(payloadKey(profileId), payload)
    }
}
