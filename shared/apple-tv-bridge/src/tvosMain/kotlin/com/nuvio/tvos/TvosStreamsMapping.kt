package com.nuvio.tvos

import com.nuvio.app.features.player.sanitizePlaybackHeaders
import com.nuvio.app.features.streams.AddonStreamGroup
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.streams.StreamsEmptyStateReason
import com.nuvio.app.features.streams.StreamsUiState
import com.nuvio.app.features.streams.streamFileSizeLabel
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_player_no_streams_found
import nuvio.composeapp.generated.resources.playback_unavailable
import nuvio.composeapp.generated.resources.playback_unavailable_message
import nuvio.composeapp.generated.resources.streams_empty_load_failed_message
import nuvio.composeapp.generated.resources.streams_empty_load_failed_title
import nuvio.composeapp.generated.resources.streams_empty_no_streams_message
import org.jetbrains.compose.resources.getString

private const val KIND_DIRECT = "Direct link"
private const val KIND_TORRENT = "Torrent"
private const val KIND_EXTERNAL = "External link"
private const val KIND_YOUTUBE = "YouTube"
private const val KIND_DEBRID = "Debrid"
private const val KIND_SERVER = "Media server"
private const val KIND_UNKNOWN = "Unknown source"
private val inAppPlayableSchemes = listOf("https://", "http://")

internal suspend fun tvosStreamsState(state: StreamsUiState): TvosStreamsState =
    TvosStreamsState(
        groups = state.groups.map(AddonStreamGroup::toTvosStreamGroup),
        isLoading = state.isAnyLoading,
        emptyState = state.tvosEmptyState(),
    )

internal fun StreamsUiState.summary(): String =
    "loading=$isAnyLoading reason=$emptyStateReason " +
        groups.joinToString { group -> "${group.addonName}:${group.streams.size}" }

private suspend fun StreamsUiState.tvosEmptyState(): TvosStreamsEmptyState? {
    if (isAnyLoading || hasAnyStreams) return null
    return when (emptyStateReason) {
        StreamsEmptyStateReason.NoAddonsInstalled,
        StreamsEmptyStateReason.NoCompatibleAddons -> TvosStreamsEmptyState(
            title = getString(Res.string.playback_unavailable),
            message = getString(Res.string.playback_unavailable_message),
            isMissingStreamAddon = true,
        )
        StreamsEmptyStateReason.StreamFetchFailed -> TvosStreamsEmptyState(
            title = getString(Res.string.streams_empty_load_failed_title),
            message = getString(Res.string.streams_empty_load_failed_message),
            isMissingStreamAddon = false,
        )
        StreamsEmptyStateReason.NoStreamsFound, null -> TvosStreamsEmptyState(
            title = getString(Res.string.compose_player_no_streams_found),
            message = getString(Res.string.streams_empty_no_streams_message),
            isMissingStreamAddon = false,
        )
    }
}

private fun AddonStreamGroup.toTvosStreamGroup(): TvosStreamGroup =
    TvosStreamGroup(
        addonId = addonId,
        addonName = addonName,
        streams = streams.mapIndexed { index, stream -> stream.toTvosStream("$addonId#$index") },
        isLoading = isLoading,
        errorMessage = error?.takeIf(String::isNotBlank),
    )

private fun StreamItem.toTvosStream(key: String): TvosStream =
    TvosStream(
        key = key,
        name = streamLabel,
        title = title?.takeIf(String::isNotBlank),
        detail = description?.takeIf(String::isNotBlank),
        badges = badges.map { badge -> badge.name }.filter(String::isNotBlank),
        sizeLabel = behaviorHints.videoSize?.let(::streamFileSizeLabel),
        kind = kindLabel(),
        url = directPlaybackUrl,
        externalUrl = externalUrl?.takeIf(String::isNotBlank),
        infoHash = p2pInfoHash,
        fileIndex = p2pFileIdx?.toString(),
        filename = behaviorHints.filename?.takeIf(String::isNotBlank),
        trackers = p2pTrackers,
        addonId = addonId,
        addonName = addonName,
        playableUrl = inAppPlayableUrl(),
        requestHeaders = sanitizePlaybackHeaders(behaviorHints.proxyHeaders?.request),
    )

private fun StreamItem.inAppPlayableUrl(): String? {
    if (serverTarget != null || isDirectDebridStream) return null
    val url = playableDirectUrl ?: return null
    return url.takeIf {
        inAppPlayableSchemes.any { scheme -> url.startsWith(scheme, ignoreCase = true) }
    }
}

private fun StreamItem.kindLabel(): String =
    when {
        serverTarget != null -> KIND_SERVER
        isDirectDebridStream -> KIND_DEBRID
        isTorrentStream -> KIND_TORRENT
        playableDirectUrl != null -> KIND_DIRECT
        youTubeIdToResolve != null -> KIND_YOUTUBE
        shouldOpenExternally -> KIND_EXTERNAL
        else -> KIND_UNKNOWN
    }
