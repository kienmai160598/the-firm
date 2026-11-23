package com.nuvio.tvos.phone

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Limits of an "Add from phone" session (docs/android-tv-rebuild.md, 5b, access model). */
object AddonPairingLimits {
    const val MAX_BODY_BYTES = 16 * 1024
    const val MAX_URLS = 10
    const val MAX_URL_LENGTH = 2_048
    const val IDLE_TIMEOUT_MILLIS = 10 * 60 * 1_000L
    const val FIRST_PORT = 8080
    const val LAST_PORT = 8089
    val allowedSchemes: Set<String> = setOf("https", "http", "stremio")
}

/** What a request to the phone server asks for, once its token has been checked. */
sealed interface AddonPairingRoute {
    /** `GET /`: the phone page. */
    data object Page : AddonPairingRoute

    /** `POST /api/addons`: a proposal of manifest URLs. */
    data object Propose : AddonPairingRoute

    /** `GET /api/status/<id>`: how a proposal went. */
    data class Status(val proposalId: Long) : AddonPairingRoute

    /** Wrong or missing token: HTTP 403, nothing changes. */
    data object Forbidden : AddonPairingRoute

    /** Right token, unknown path or method: HTTP 404. */
    data object NotFound : AddonPairingRoute
}

/**
 * Routes one request. The token is checked first, on every path, so a request without it learns
 * nothing (not even which paths exist).
 */
fun addonPairingRoute(method: String, path: String, token: String?, expected: AddonPairingToken): AddonPairingRoute {
    if (!expected.matches(token)) return AddonPairingRoute.Forbidden
    val isGet = method.equals("GET", ignoreCase = true)
    val isPost = method.equals("POST", ignoreCase = true)
    return when {
        isGet && (path == "/" || path.isEmpty()) -> AddonPairingRoute.Page

        isPost && path == PROPOSE_PATH -> AddonPairingRoute.Propose

        isGet && path.startsWith(STATUS_PREFIX) ->
            path.removePrefix(STATUS_PREFIX).toLongOrNull()?.let(AddonPairingRoute::Status) ?: AddonPairingRoute.NotFound

        else -> AddonPairingRoute.NotFound
    }
}

/** Why a proposal's body was refused before it was read. */
enum class AddonBodyProblem { MissingLength, TooLarge }

/** Checks the declared `Content-Length` of a proposal before any byte of the body is read. */
fun addonBodyProblem(contentLength: Long?): AddonBodyProblem? = when {
    contentLength == null || contentLength < 0 -> AddonBodyProblem.MissingLength
    contentLength > AddonPairingLimits.MAX_BODY_BYTES -> AddonBodyProblem.TooLarge
    else -> null
}

/** A proposal body, checked. */
sealed interface AddonProposalParse {
    data class Accepted(val urls: List<String>) : AddonProposalParse

    /** [message] is shown on the phone page. */
    data class Rejected(val message: String) : AddonProposalParse
}

/**
 * Parses a proposal body, `{"urls": ["https://.../manifest.json", ...]}`, and applies the URL
 * limits: at most [AddonPairingLimits.MAX_URLS] URLs, each at most
 * [AddonPairingLimits.MAX_URL_LENGTH] characters, scheme `https`, `http` or `stremio`, no spaces.
 * Blank entries and repeats are dropped. The manifest itself is checked by core when it installs.
 */
fun parseAddonProposal(body: String): AddonProposalParse {
    if (body.encodeToByteArray().size > AddonPairingLimits.MAX_BODY_BYTES) return rejected(AddonPairingMessages.TOO_LARGE)
    val urls = proposalUrls(body) ?: return rejected("Send the manifest URLs as {\"urls\": [...]}.")
    val cleaned = urls.map(String::trim).filter(String::isNotEmpty).distinct()
    return when {
        cleaned.isEmpty() -> rejected("Paste at least one manifest URL.")
        cleaned.size > AddonPairingLimits.MAX_URLS -> rejected("Send at most ${AddonPairingLimits.MAX_URLS} URLs at a time.")
        else -> cleaned.firstNotNullOfOrNull(::urlProblem)?.let(::rejected) ?: AddonProposalParse.Accepted(cleaned)
    }
}

private fun proposalUrls(body: String): List<String>? {
    val root = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return null
    val array = root["urls"] as? JsonArray ?: return null
    return array.map { element -> (element as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content ?: return null }
}

private fun urlProblem(url: String): String? {
    val scheme = url.substringBefore("://", missingDelimiterValue = "").lowercase()
    return when {
        url.length > AddonPairingLimits.MAX_URL_LENGTH -> "A URL is longer than ${AddonPairingLimits.MAX_URL_LENGTH} characters."
        scheme !in AddonPairingLimits.allowedSchemes -> "Only https, http and stremio links work: $url"
        url.substringAfter("://").isEmpty() -> "This link has no address: $url"
        url.any(Char::isWhitespace) -> "A URL cannot contain spaces: $url"
        else -> null
    }
}

private fun rejected(message: String) = AddonProposalParse.Rejected(message)

private const val PROPOSE_PATH = "/api/addons"
private const val STATUS_PREFIX = "/api/status/"
