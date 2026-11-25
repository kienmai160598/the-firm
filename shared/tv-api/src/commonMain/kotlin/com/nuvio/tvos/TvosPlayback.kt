package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.player.PlayerPlaybackSnapshot
import com.nuvio.app.features.watching.domain.isShortPlaceholderDuration
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.WatchProgressPlaybackSession
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.tvos.rules.ResumeStart
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object TvosPlayback {
    private val mainScope = MainScope()

    fun resumePoint(target: TvosStreamTarget, onResult: (TvosResumePoint?) -> Unit) {
        mainScope.launch { onResult(tvosResumePoint(savedProgress(target))) }
    }

    fun resumePositionMs(target: TvosStreamTarget, actualDurationMs: Long): Long {
        val entry = resumableProgress(target) ?: return 0L
        return entry.resolveResumePosition(actualDurationMs)
    }

    /**
     * The resume position when the saved progress gives it without the stream's length, so a
     * player can open the stream there; null when [resumePositionMs] has to wait for the length.
     */
    fun resumePositionBeforeLoadMs(target: TvosStreamTarget): Long? {
        val entry = resumableProgress(target) ?: return null
        return ResumeStart.positionBeforeLoadMs(entry.lastPositionMs, entry.durationMs)
    }

    fun startSession(target: TvosStreamTarget, stream: TvosStream): TvosPlaybackSession =
        TvosPlaybackSession(watchProgressSession(target, stream)) { TvosLastLink.remember(target, stream) }

    private fun resumableProgress(target: TvosStreamTarget): WatchProgressEntry? =
        savedProgress(target)?.takeUnless(WatchProgressEntry::isCompleted)

    private fun savedProgress(target: TvosStreamTarget): WatchProgressEntry? =
        WatchProgressRepository.progressForVideo(
            videoId = target.videoId,
            parentMetaId = target.parentMetaId,
            seasonNumber = target.seasonNumber,
            episodeNumber = target.episodeNumber,
        )
}

class TvosPlaybackSession internal constructor(
    private val session: WatchProgressPlaybackSession,
    /** Runs at the first saved progress: the link played, so Continue Watching may reuse it. */
    private val onPlayed: () -> Unit,
) {
    private val log = Logger.withTag("TvosPlayback")
    private var hasPlayed = false

    fun save(
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean,
        isEnded: Boolean,
        syncRemote: Boolean,
    ): Boolean {
        if (durationMs <= 0L || isShortPlaceholderDuration(durationMs)) return false
        val snapshot = PlayerPlaybackSnapshot(
            isLoading = false,
            isPlaying = isPlaying,
            isEnded = isEnded,
            durationMs = durationMs,
            positionMs = positionMs,
        )
        WatchProgressRepository.upsertPlaybackProgress(session, snapshot, syncRemote = syncRemote)
        log.i { "Saved ${session.videoId} at $positionMs of $durationMs ended=$isEnded" }
        if (!hasPlayed) {
            hasPlayed = true
            onPlayed()
        }
        return true
    }
}
