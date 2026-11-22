package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.player.PlayerPlaybackSnapshot
import com.nuvio.app.features.watching.domain.isShortPlaceholderDuration
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.WatchProgressPlaybackSession
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object TvosPlayback {
    private val mainScope = MainScope()

    fun resumePoint(target: TvosStreamTarget, onResult: (TvosResumePoint?) -> Unit) {
        mainScope.launch { onResult(tvosResumePoint(savedProgress(target))) }
    }

    fun resumePositionMs(target: TvosStreamTarget, actualDurationMs: Long): Long {
        val entry = savedProgress(target)?.takeUnless(WatchProgressEntry::isCompleted) ?: return 0L
        return entry.resolveResumePosition(actualDurationMs)
    }

    fun startSession(target: TvosStreamTarget, stream: TvosStream): TvosPlaybackSession =
        TvosPlaybackSession(watchProgressSession(target, stream))

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
) {
    private val log = Logger.withTag("TvosPlayback")

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
        return true
    }
}
