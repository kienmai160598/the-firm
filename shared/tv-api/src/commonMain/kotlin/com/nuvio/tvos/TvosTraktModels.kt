package com.nuvio.tvos

/** Where the Trakt account is: what the Account page shows. */
enum class TvosTraktPhase {
    /** The build has no Trakt client id and secret, so sign in cannot start. */
    NOT_CONFIGURED,
    SIGNED_OUT,

    /** Asking Trakt for a code. */
    STARTING,

    /** Showing [TvosTraktState.userCode] until the user approves it on a phone or computer. */
    WAITING_FOR_APPROVAL,
    SIGNED_IN,
}

/** Why the last sign in stopped. */
enum class TvosTraktFailure {
    /** The code ran out before it was approved. */
    EXPIRED,

    /** The user chose Deny on trakt.tv. */
    DENIED,

    /** Trakt or the network failed, or the code was invalid or already used. */
    FAILED,
}

data class TvosTraktState(
    val phase: TvosTraktPhase,
    val userCode: String? = null,
    val verificationUrl: String? = null,
    val username: String? = null,
    val failure: TvosTraktFailure? = null,
)
