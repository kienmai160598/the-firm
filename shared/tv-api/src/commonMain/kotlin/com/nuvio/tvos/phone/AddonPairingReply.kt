package com.nuvio.tvos.phone

import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * The JSON bodies the phone page reads. Names and errors are data for the page to show as text,
 * never markup.
 */
object AddonPairingReply {
    /** `200` to a proposal: `{"id": 3, "status": "pending", "urls": [...]}`. */
    fun proposed(proposal: AddonProposal): String = buildJsonObject {
        put("id", proposal.id)
        put("status", "pending")
        putJsonArray("urls") { proposal.urls.forEach(::add) }
    }.toString()

    /**
     * A poll: `{"status": "done", "results": [{"url": ..., "ok": true, "name": ...}]}`. Status is
     * `pending`, `installing`, `done`, `cancelled` or `unknown`.
     */
    fun status(status: AddonProposalStatus): String = buildJsonObject {
        put("status", status.wireName())
        if (status is AddonProposalStatus.Done) {
            putJsonArray("results") { status.outcomes.forEach { outcome -> addJsonObject { putOutcome(outcome) } } }
        }
    }.toString()

    /** Any refusal: `{"error": "..."}`. */
    fun error(message: String): String = buildJsonObject { put("error", message) }.toString()

    private fun JsonObjectBuilder.putOutcome(outcome: AddonInstallOutcome) {
        put("url", outcome.url)
        when (outcome) {
            is AddonInstallOutcome.Installed -> {
                put("ok", true)
                put("name", outcome.name)
            }

            is AddonInstallOutcome.Failed -> {
                put("ok", false)
                put("error", outcome.message)
            }
        }
    }

    private fun AddonProposalStatus.wireName(): String = when (this) {
        AddonProposalStatus.Pending -> "pending"
        AddonProposalStatus.Installing -> "installing"
        is AddonProposalStatus.Done -> "done"
        AddonProposalStatus.Cancelled -> "cancelled"
        AddonProposalStatus.Unknown -> "unknown"
    }
}

/** Messages the server sends with its refusals, shown on the phone page. */
object AddonPairingMessages {
    const val FORBIDDEN = "This link is not valid any more. Scan the code on the TV again."
    const val CONFLICT = "The TV is already asking about other addons. Answer it on the TV first."
    const val CLOSED = "Add from phone is closed on the TV."
    const val NOT_FOUND = "Not found."
    const val LENGTH_REQUIRED = "The request has no length."
    const val TOO_LARGE = "The request is too large."
}
