package com.nuvio.tvos.rules

import com.nuvio.tvos.TvosStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * How the link a title last played from is kept (TvosLastLink): the whole stream as JSON with the
 * time it was saved, so a Continue Watching card can play it again exactly as it was picked, a
 * torrent by its info hash and a direct link by its URL. A link older than [MAX_AGE_MS] is not
 * used: debrid and addon links expire.
 */
object LastLink {
    const val MAX_AGE_MS: Long = 24L * 60 * 60 * 1000

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(stream: TvosStream, savedAtMs: Long): String =
        json.encodeToString(SavedLink.serializer(), SavedLink(savedAtMs, stream.toSaved()))

    /** The saved stream, or null when the payload is unreadable or older than [MAX_AGE_MS]. */
    fun decode(payload: String, nowMs: Long): TvosStream? {
        val saved = runCatching { json.decodeFromString(SavedLink.serializer(), payload) }.getOrNull()
            ?: return null
        val age = nowMs - saved.savedAtMs
        if (age < 0 || age > MAX_AGE_MS) return null
        return saved.stream.toStream()
    }
}

@Serializable
private data class SavedLink(val savedAtMs: Long, val stream: SavedStream)

@Serializable
private data class SavedStream(
    val key: String,
    val name: String,
    val title: String? = null,
    val detail: String? = null,
    val badges: List<String> = emptyList(),
    val sizeLabel: String? = null,
    val kind: String,
    val url: String? = null,
    val externalUrl: String? = null,
    val infoHash: String? = null,
    val fileIndex: String? = null,
    val filename: String? = null,
    val trackers: List<String> = emptyList(),
    val addonId: String,
    val addonName: String,
    val playableUrl: String? = null,
    val requestHeaders: Map<String, String> = emptyMap(),
)

private fun TvosStream.toSaved() = SavedStream(
    key, name, title, detail, badges, sizeLabel, kind, url, externalUrl, infoHash, fileIndex,
    filename, trackers, addonId, addonName, playableUrl, requestHeaders,
)

private fun SavedStream.toStream() = TvosStream(
    key, name, title, detail, badges, sizeLabel, kind, url, externalUrl, infoHash, fileIndex,
    filename, trackers, addonId, addonName, playableUrl, requestHeaders,
)
