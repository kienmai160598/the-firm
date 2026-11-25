package com.nuvio.tvos

/**
 * One Streams screen's try of the link a Continue Watching card's title last played from (AC H4):
 * [take] gives it once; when it then fails to play, [failed] forgets it, so the list is the way
 * on. Each app words the notice itself, as for its other Streams failures.
 */
class TvosLastLinkAttempt(private val target: TvosStreamTarget) {
    private var recalledKey: String? = null
    private var hasTried = false

    /** The link to play now, the first time only; null when none was played in the last day. */
    fun take(): TvosStream? {
        if (hasTried) return null
        hasTried = true
        return TvosLastLink.recall(target).also { stream -> recalledKey = stream?.key }
    }

    /** True when the stream of [streamKey] that did not play was the last link, now forgotten. */
    fun failed(streamKey: String): Boolean {
        if (recalledKey == null || streamKey != recalledKey) return false
        recalledKey = null
        TvosLastLink.forget(target)
        return true
    }
}
