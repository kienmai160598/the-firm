package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.details.selectHeroTrailer
import com.nuvio.app.features.details.youtubePlaybackUrl
import com.nuvio.app.features.trailer.TRAILER_REQUEST_TIMEOUT_MS
import com.nuvio.app.features.trailer.TrailerExtractionPlatform
import com.nuvio.app.features.trailer.TrailerPlaybackResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** YouTube stream links expire after a few hours, so a resolved link is kept for less than that. */
private val TRAILER_LINK_LIFETIME = 1.hours

/**
 * A trailer AVPlayer can play: its link, and for an HLS link the master playlist cut down to its
 * sharpest variants (see [sharpestTierPlaylist]), which the app plays in place of the link.
 */
class TvosTrailerLink(
    val url: String,
    val playlist: String?,
)

/**
 * A playable trailer link for a Home spotlight item, resolved from its YouTube trailer. Only
 * links that were found are kept, so a lookup made while the addons were not ready, or one that
 * failed, is tried again next time. Lookups for the same item share one request, so a prefetch
 * and the focus that follows it cost one extraction.
 */
object TvosTrailers {
    private data class Resolved(
        val link: TvosTrailerLink,
        val at: TimeMark,
    )

    private val log = Logger.withTag("TvosTrailers")
    private val mainScope = MainScope()
    private val resolved = mutableMapOf<String, Resolved>()
    private val inFlight = mutableMapOf<String, Deferred<TvosTrailerLink?>>()

    /** Calls [onResult] with a trailer AVPlayer can play, or null when the item has no trailer. */
    fun resolve(
        type: String,
        id: String,
        onResult: (TvosTrailerLink?) -> Unit,
    ): TvosCancellable {
        val job = mainScope.launch { onResult(lookup(type, id).await()) }
        return TvosCancellable(job)
    }

    /** Starts a lookup that outlives the caller, so the link is ready when the item gets focus. */
    fun prefetch(
        type: String,
        id: String,
    ) {
        lookup(type, id)
    }

    private fun lookup(
        type: String,
        id: String,
    ): Deferred<TvosTrailerLink?> {
        val key = "$type:$id"
        resolved[key]?.takeIf { it.at.elapsedNow() < TRAILER_LINK_LIFETIME }?.let {
            return CompletableDeferred(it.link)
        }
        return inFlight.getOrPut(key) {
            mainScope.async {
                val link = resolveLink(type, id)?.let { url -> TvosTrailerLink(url, sharpestPlaylist(url)) }
                if (link != null) resolved[key] = Resolved(link, TimeSource.Monotonic.markNow())
                inFlight.remove(key)
                link
            }
        }
    }

    private suspend fun resolveLink(
        type: String,
        id: String,
    ): String? =
        try {
            TvosHome.awaitAddonsReadyForMeta()
            val trailer = MetaDetailsRepository.fetch(type, id)?.trailers?.let(::selectHeroTrailer)
            trailer?.let { TrailerPlaybackResolver.resolveSingleUrlFromYouTubeUrl(it.youtubePlaybackUrl()) }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            log.e(error) { "Trailer for $type $id failed" }
            null
        }

    /** Null keeps the original link: not HLS, or the playlist could not be read or cut down. */
    private suspend fun sharpestPlaylist(url: String): String? {
        if (!isHlsMasterLink(url)) return null
        return try {
            val response = TrailerExtractionPlatform.performRequest(
                url = url,
                method = "GET",
                headers = TrailerExtractionPlatform.defaultHeaders,
                body = null,
                timeoutMillis = TRAILER_REQUEST_TIMEOUT_MS,
            )
            if (response.ok) sharpestTierPlaylist(response.body) else null
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            log.w(error) { "Trailer playlist $url could not be read; playing the original" }
            null
        }
    }
}
