package com.nuvio.tvos.phone

/** Manifest URLs a phone sent, waiting for the remote. [id] is what the phone polls. */
data class AddonProposal(val id: Long, val urls: List<String>)

/** How one proposed URL went, from `TvosAddons.install`. */
sealed interface AddonInstallOutcome {
    val url: String

    data class Installed(override val url: String, val name: String) : AddonInstallOutcome

    data class Failed(override val url: String, val message: String) : AddonInstallOutcome
}

/**
 * Where a session is (docs/android-tv-rebuild.md, 5b):
 * `Off -> Waiting -> Proposed -> Installing -> Done -> Waiting`, `Proposed -> Waiting` on Cancel,
 * any state `-> Off`. [Done] takes a new proposal as [Waiting] does; it only keeps the last
 * results on the TV screen until then.
 */
sealed interface AddonPairingPhase {
    data object Off : AddonPairingPhase

    data object Waiting : AddonPairingPhase

    data class Proposed(val proposal: AddonProposal) : AddonPairingPhase

    data class Installing(val proposal: AddonProposal) : AddonPairingPhase

    data class Done(val proposal: AddonProposal, val outcomes: List<AddonInstallOutcome>) : AddonPairingPhase
}

/** What the phone sees when it polls a proposal. */
sealed interface AddonProposalStatus {
    data object Pending : AddonProposalStatus

    data object Installing : AddonProposalStatus

    data class Done(val outcomes: List<AddonInstallOutcome>) : AddonProposalStatus

    data object Cancelled : AddonProposalStatus

    /** Not a proposal of this session, or too old to remember. */
    data object Unknown : AddonProposalStatus
}

sealed interface AddonProposeResult {
    data class Accepted(val proposal: AddonProposal) : AddonProposeResult

    /** One proposal is already open on the TV: HTTP 409. */
    data object Conflict : AddonProposeResult

    /** The session is off. */
    data object Closed : AddonProposeResult
}

/**
 * One "Add from phone" session as an immutable value: every transition returns a new session, so
 * the server threads and the TV can share it through one atomic reference (compare and set) and
 * two proposals cannot both pass the 409 check.
 *
 * Transitions that do not apply (a confirm for another proposal, a finish after a cancel) return
 * the session unchanged. Times are passed in, so the 10 minute idle rule is testable.
 */
data class AddonPairingSession(
    val phase: AddonPairingPhase = AddonPairingPhase.Off,
    val lastRequestAtMillis: Long = 0,
    private val nextProposalId: Long = 1,
    private val finished: Map<Long, AddonProposalStatus> = emptyMap()
) {
    /** A fresh session that waits for a phone. */
    fun start(nowMillis: Long): AddonPairingSession = AddonPairingSession(phase = AddonPairingPhase.Waiting, lastRequestAtMillis = nowMillis)

    /** Any request with the right token keeps the session alive. */
    fun touch(nowMillis: Long): AddonPairingSession = if (phase == AddonPairingPhase.Off) this else copy(lastRequestAtMillis = nowMillis)

    /** True when the server should stop: no request for [AddonPairingLimits.IDLE_TIMEOUT_MILLIS]. */
    fun isIdle(nowMillis: Long): Boolean = phase != AddonPairingPhase.Off && nowMillis - lastRequestAtMillis >= AddonPairingLimits.IDLE_TIMEOUT_MILLIS

    fun propose(urls: List<String>, nowMillis: Long): Pair<AddonPairingSession, AddonProposeResult> = when (phase) {
        AddonPairingPhase.Off -> this to AddonProposeResult.Closed

        is AddonPairingPhase.Proposed, is AddonPairingPhase.Installing -> touch(nowMillis) to AddonProposeResult.Conflict

        AddonPairingPhase.Waiting, is AddonPairingPhase.Done -> {
            val proposal = AddonProposal(id = nextProposalId, urls = urls)
            val next = copy(
                phase = AddonPairingPhase.Proposed(proposal),
                lastRequestAtMillis = nowMillis,
                nextProposalId = nextProposalId + 1
            )
            next to AddonProposeResult.Accepted(proposal)
        }
    }

    /** Install pressed on the TV. */
    fun confirm(proposalId: Long): AddonPairingSession {
        val proposed = phase as? AddonPairingPhase.Proposed ?: return this
        if (proposed.proposal.id != proposalId) return this
        return copy(phase = AddonPairingPhase.Installing(proposed.proposal))
    }

    /** Cancel pressed on the TV: nothing is installed and the phone is told so. */
    fun cancel(proposalId: Long): AddonPairingSession {
        val proposed = phase as? AddonPairingPhase.Proposed ?: return this
        if (proposed.proposal.id != proposalId) return this
        return copy(phase = AddonPairingPhase.Waiting, finished = remember(proposalId, AddonProposalStatus.Cancelled))
    }

    /** Every URL of the proposal went through `TvosAddons.install`. */
    fun finish(proposalId: Long, outcomes: List<AddonInstallOutcome>): AddonPairingSession {
        val installing = phase as? AddonPairingPhase.Installing ?: return this
        if (installing.proposal.id != proposalId) return this
        return copy(
            phase = AddonPairingPhase.Done(installing.proposal, outcomes),
            finished = remember(proposalId, AddonProposalStatus.Done(outcomes))
        )
    }

    fun stop(): AddonPairingSession = copy(phase = AddonPairingPhase.Off)

    fun status(proposalId: Long): AddonProposalStatus {
        val current = phase
        return when {
            current is AddonPairingPhase.Proposed && current.proposal.id == proposalId -> AddonProposalStatus.Pending
            current is AddonPairingPhase.Installing && current.proposal.id == proposalId -> AddonProposalStatus.Installing
            else -> finished[proposalId] ?: AddonProposalStatus.Unknown
        }
    }

    /** Keeps the last few finished proposals, so a phone that polls late still gets its answer. */
    private fun remember(proposalId: Long, status: AddonProposalStatus): Map<Long, AddonProposalStatus> = (finished + (proposalId to status)).entries
        .sortedByDescending { entry -> entry.key }
        .take(REMEMBERED_PROPOSALS)
        .associate { entry -> entry.key to entry.value }

    private companion object {
        const val REMEMBERED_PROPOSALS = 10
    }
}
