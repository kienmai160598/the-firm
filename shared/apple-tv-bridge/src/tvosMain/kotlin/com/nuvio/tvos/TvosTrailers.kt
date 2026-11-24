package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.details.selectHeroTrailer
import com.nuvio.app.features.details.youtubePlaybackUrl
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
 * A playable trailer link for a Home spotlight item, resolved from its YouTube trailer. Only
 * links that were found are kept, so a lookup made while the addons were not ready, or one that
 * failed, is tried again next time. Lookups for the same item share one request, so a prefetch
 * and the focus that follows it cost one extraction.
 */
object TvosTrailers {
    private data class Resolved(
        val url: String,
        val at: TimeMark,
    )

    private val log = Logger.withTag("TvosTrailers")
    private val mainScope = MainScope()
    private val resolved = mutableMapOf<String, Resolved>()
    private val inFlight = mutableMapOf<String, Deferred<String?>>()

    /** Calls [onResult] with a link AVPlayer can play, or null when the item has no trailer. */
    fun resolve(
        type: String,
        id: String,
        onResult: (String?) -> Unit,
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
    ): Deferred<String?> {
        val key = "$type:$id"
        resolved[key]?.takeIf { it.at.elapsedNow() < TRAILER_LINK_LIFETIME }?.let {
            return CompletableDeferred(it.url)
        }
        return inFlight.getOrPut(key) {
            mainScope.async {
                val url = resolveLink(type, id)
                if (url != null) resolved[key] = Resolved(url, TimeSource.Monotonic.markNow())
                inFlight.remove(key)
                url
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
}
