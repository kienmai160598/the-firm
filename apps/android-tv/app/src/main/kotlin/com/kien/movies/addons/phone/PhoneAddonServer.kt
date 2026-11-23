package com.kien.movies.addons.phone

import com.nuvio.tvos.phone.AddonBodyProblem
import com.nuvio.tvos.phone.AddonInstallOutcome
import com.nuvio.tvos.phone.AddonPairingLimits
import com.nuvio.tvos.phone.AddonPairingMessages
import com.nuvio.tvos.phone.AddonPairingPhase
import com.nuvio.tvos.phone.AddonPairingReply
import com.nuvio.tvos.phone.AddonPairingRoute
import com.nuvio.tvos.phone.AddonPairingSession
import com.nuvio.tvos.phone.AddonPairingToken
import com.nuvio.tvos.phone.AddonProposalParse
import com.nuvio.tvos.phone.AddonProposeResult
import com.nuvio.tvos.phone.addonBodyProblem
import com.nuvio.tvos.phone.addonPairingRoute
import com.nuvio.tvos.phone.parseAddonProposal
import fi.iki.elonen.NanoHTTPD
import java.io.DataInputStream
import java.io.IOException
import java.security.SecureRandom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Installs the confirmed URLs through the same path as typing them, then calls `onDone`. */
typealias PhoneAddonInstaller = (urls: List<String>, onDone: (List<AddonInstallOutcome>) -> Unit) -> Unit

/**
 * The local HTTP server of "Add from phone" (docs/android-tv-rebuild.md, 5b). It serves the phone
 * page, takes proposals and answers polls; the session rules (token, limits, 409) live in
 * `shared/tv-api/.../phone`. Plain JVM, no Android API, so it is tested on the JVM.
 *
 * The phone only proposes: nothing is installed until [confirm] is called from the TV's dialog.
 * Every response closes its connection, so a refused body is never left unread on a kept-alive
 * socket.
 */
class PhoneAddonServer private constructor(
    private val host: String,
    port: Int,
    private val token: AddonPairingToken,
    private val clock: () -> Long,
    private val install: PhoneAddonInstaller
) : NanoHTTPD(host, port) {
    private val sessionState = MutableStateFlow(AddonPairingSession().start(clock()))

    /** The session the TV draws: waiting, the proposal for the dialog, installing, results. */
    val session: StateFlow<AddonPairingSession> = sessionState.asStateFlow()

    /** What the QR code holds and the TV shows as text. */
    val address: String get() = "http://$host:$listeningPort/?t=${token.value}"

    /** Install pressed on the TV: installs the proposal's URLs and reports back to the phone. */
    fun confirm(proposalId: Long) {
        val before = transition { session -> session.confirm(proposalId) }
        val proposal = (before as? AddonPairingPhase.Proposed)?.proposal?.takeIf { proposed -> proposed.id == proposalId } ?: return
        install(proposal.urls) { outcomes -> sessionState.update { session -> session.finish(proposalId, outcomes) } }
    }

    /** Cancel pressed on the TV, or Back on the dialog. */
    fun cancel(proposalId: Long) = sessionState.update { session -> session.cancel(proposalId) }

    /** True when no request came for 10 minutes; the panel then closes and calls [close]. */
    fun isIdle(): Boolean = sessionState.value.isIdle(clock())

    /** Stops answering: the address no longer responds. Blocks briefly; call off the main thread. */
    fun close() {
        sessionState.update(AddonPairingSession::stop)
        stop()
    }

    override fun serve(request: IHTTPSession): Response {
        val token = request.parameters[TOKEN_PARAMETER]?.firstOrNull()
        val response = when (val route = addonPairingRoute(request.method.name, request.uri, token, this.token)) {
            AddonPairingRoute.Forbidden -> json(Response.Status.FORBIDDEN, AddonPairingReply.error(AddonPairingMessages.FORBIDDEN))

            AddonPairingRoute.NotFound -> json(Response.Status.NOT_FOUND, AddonPairingReply.error(AddonPairingMessages.NOT_FOUND))

            AddonPairingRoute.Page -> {
                touch()
                newFixedLengthResponse(Response.Status.OK, HTML, PhoneAddonPage.html)
            }

            AddonPairingRoute.Propose -> {
                touch()
                propose(request)
            }

            is AddonPairingRoute.Status -> {
                touch()
                json(Response.Status.OK, AddonPairingReply.status(sessionState.value.status(route.proposalId)))
            }
        }
        response.addHeader("Cache-Control", "no-store")
        response.closeConnection(true)
        return response
    }

    private fun propose(request: IHTTPSession): Response {
        val length = request.headers[CONTENT_LENGTH]?.trim()?.toLongOrNull()
        when (addonBodyProblem(length)) {
            AddonBodyProblem.MissingLength -> return json(Response.Status.LENGTH_REQUIRED, AddonPairingReply.error(AddonPairingMessages.LENGTH_REQUIRED))
            AddonBodyProblem.TooLarge -> return json(Response.Status.PAYLOAD_TOO_LARGE, AddonPairingReply.error(AddonPairingMessages.TOO_LARGE))
            null -> Unit
        }
        val body = readBody(request, checkNotNull(length).toInt()) ?: return json(Response.Status.BAD_REQUEST, AddonPairingReply.error(AddonPairingMessages.TOO_LARGE))
        val urls = when (val parsed = parseAddonProposal(body)) {
            is AddonProposalParse.Rejected -> return json(Response.Status.BAD_REQUEST, AddonPairingReply.error(parsed.message))
            is AddonProposalParse.Accepted -> parsed.urls
        }
        return when (val result = proposeAtomically(urls)) {
            is AddonProposeResult.Accepted -> json(Response.Status.OK, AddonPairingReply.proposed(result.proposal))
            AddonProposeResult.Conflict -> json(Response.Status.CONFLICT, AddonPairingReply.error(AddonPairingMessages.CONFLICT))
            AddonProposeResult.Closed -> json(Response.Status.GONE, AddonPairingReply.error(AddonPairingMessages.CLOSED))
        }
    }

    /** One compare and set, so two phones posting at once cannot both get past the 409 check. */
    private fun proposeAtomically(urls: List<String>): AddonProposeResult {
        while (true) {
            val current = sessionState.value
            val (next, result) = current.propose(urls, clock())
            if (sessionState.compareAndSet(current, next)) return result
        }
    }

    /** Applies [change] atomically and returns the phase it was applied to. */
    private fun transition(change: (AddonPairingSession) -> AddonPairingSession): AddonPairingPhase {
        while (true) {
            val current = sessionState.value
            if (sessionState.compareAndSet(current, change(current))) return current.phase
        }
    }

    private fun touch() = sessionState.update { session -> session.touch(clock()) }

    /** Reads exactly [length] bytes (already capped at 16 KB), or null when the phone sent fewer. */
    private fun readBody(request: IHTTPSession, length: Int): String? = try {
        val bytes = ByteArray(length)
        DataInputStream(request.inputStream).readFully(bytes)
        bytes.decodeToString()
    } catch (_: IOException) {
        null
    }

    private fun json(status: Response.Status, body: String): Response = newFixedLengthResponse(status, JSON, body)

    companion object {
        private const val TOKEN_PARAMETER = "t"
        private const val CONTENT_LENGTH = "content-length"
        private const val JSON = "application/json; charset=utf-8"
        private const val HTML = "text/html; charset=utf-8"
        private val defaultPorts = AddonPairingLimits.FIRST_PORT..AddonPairingLimits.LAST_PORT
        private val random = SecureRandom()

        /**
         * Starts a server bound to [host] (the TV's LAN address) on the first free port of
         * [ports], with a new token. Null when every port is taken. Binds a socket: call off the
         * main thread.
         */
        fun start(
            host: String,
            clock: () -> Long,
            install: PhoneAddonInstaller,
            ports: Iterable<Int> = defaultPorts
        ): PhoneAddonServer? {
            val token = AddonPairingToken.fromRandomBytes(ByteArray(AddonPairingToken.BYTE_COUNT).also(random::nextBytes))
            for (port in ports) {
                val server = PhoneAddonServer(host, port, token, clock, install)
                try {
                    server.start(SOCKET_READ_TIMEOUT, true)
                    return server
                } catch (_: IOException) {
                    // Port taken: try the next one.
                }
            }
            return null
        }
    }
}
