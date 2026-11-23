package com.nuvio.tvos

import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.streams.resolveStreamResumeState
import com.nuvio.app.features.streams.toPlaybackClock
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.WatchProgressPlaybackSession
import kotlin.math.roundToInt
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.cw_action_start_from_beginning
import nuvio.composeapp.generated.resources.streams_resume_from_percent
import nuvio.composeapp.generated.resources.streams_resume_from_time
import org.jetbrains.compose.resources.getString

internal suspend fun tvosResumePoint(entry: WatchProgressEntry?): TvosResumePoint? {
    val resumable = entry?.takeUnless(WatchProgressEntry::isCompleted) ?: return null
    val state = resolveStreamResumeState(
        progress = resumable,
        initialPositionMs = null,
        initialProgressFraction = null,
        startFromBeginning = false,
    )
    val label = when {
        state.progressFraction != null -> getString(
            Res.string.streams_resume_from_percent,
            (state.progressFraction * 100f).roundToInt(),
        )
        state.positionMs != null -> getString(
            Res.string.streams_resume_from_time,
            state.positionMs.toPlaybackClock(),
        )
        else -> return null
    }
    return TvosResumePoint(
        label = label,
        startOverLabel = getString(Res.string.cw_action_start_from_beginning),
        watchedFraction = resumable.progressFraction,
    )
}

internal fun watchProgressSession(
    target: TvosStreamTarget,
    stream: TvosStream,
): WatchProgressPlaybackSession =
    WatchProgressPlaybackSession(
        profileId = ProfileRepository.activeProfileId,
        contentType = target.type,
        parentMetaId = target.parentMetaId,
        parentMetaType = target.type,
        videoId = target.videoId,
        title = target.title,
        logo = target.logoUrl,
        poster = target.posterUrl,
        background = target.backdropUrl,
        seasonNumber = target.seasonNumber,
        episodeNumber = target.episodeNumber,
        episodeTitle = target.episodeTitle,
        episodeThumbnail = target.episodeThumbnailUrl,
        providerName = stream.addonName,
        providerAddonId = stream.addonId,
        lastStreamTitle = stream.name,
        lastStreamSubtitle = stream.detail,
        lastSourceUrl = stream.playableUrl,
    )
