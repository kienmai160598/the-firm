package com.kien.movies.streams

import androidx.compose.runtime.Immutable
import com.kien.movies.player.StreamNotice
import com.nuvio.tvos.TvosResumePoint
import com.nuvio.tvos.TvosStream
import com.nuvio.tvos.TvosTorrentStatus
import com.nuvio.tvos.rules.StreamFeatures
import com.nuvio.tvos.rules.StreamFileFormat

// The rows' state and the rule for a press (tvOS StreamsRow.swift, StreamGroupList.swift and the
// select path of StreamsView.swift). Row states, docs/android-tv-rebuild.md section 3:
// Idle -> Checking -> Playing -> Idle; Idle -> TorrentStarting -> Checking -> Playing; any -> Notice
// (the info sheet). A press is ignored while another row is not Idle.

/** What the trailing side of the pressed row shows instead of size and seeders. */
@Immutable
data class StreamRowProgress(val title: String, val detail: String? = null, val fraction: Float? = null) {
    companion object {
        val checking = StreamRowProgress(title = "Checking stream")
        val startingTorrent = StreamRowProgress(title = "Starting torrent")
    }
}

/**
 * The pressed row while its torrent starts (tvOS StreamCheckingStatus): the engine's step, and
 * "Starting torrent" before the first tick. The engine reports no peers, speed or buffer until E4,
 * so the row shows neither a detail line nor a bar (AC T1).
 */
fun torrentProgress(status: TvosTorrentStatus?): StreamRowProgress = StreamRowProgress(
    title = status?.step ?: StreamRowProgress.startingTorrent.title
)

/** The one row that is not Idle. Every other row dims while it is set (AC S6). */
@Immutable
data class StreamSelection(val streamKey: String, val progress: StreamRowProgress) {
    fun progressFor(key: String): StreamRowProgress? = progress.takeIf { key == streamKey }

    fun dims(key: String): Boolean = key != streamKey
}

/** One stream as a row shows it; [features] is parsed once per state, not per frame. */
@Immutable
data class StreamRowItem(
    val stream: TvosStream,
    val features: StreamFeatures,
    val oneLineName: String,
    /** Up to four capabilities, then the file format (MKV, MP4, HLS), as the tag line shows them. */
    val tags: List<String>
) {
    val key: String get() = stream.key
    val isTorrent: Boolean get() = stream.infoHash != null
}

/** One addon's section: its rows, or why it found none. */
@Immutable
data class StreamGroupItem(
    val addonId: String,
    val addonName: String,
    val errorMessage: String?,
    val rows: List<StreamRowItem>
)

/** The info sheet of [stream], with [notice] when playback failed or could not start (S4, P4). */
@Immutable
data class StreamSheet(val stream: TvosStream, val notice: StreamNotice? = null)

/** A press waiting for Resume or Start Over (S5). */
@Immutable
data class PendingResume(val stream: TvosStream, val point: TvosResumePoint)

/** What a press on a row leads to. */
sealed interface StreamPress {
    /** Another row is busy or the player is up. */
    data object Ignored : StreamPress

    data class OpensSheet(val sheet: StreamSheet) : StreamPress

    /** The row shows "Checking stream" while the resume point loads. */
    data class Checks(val stream: TvosStream) : StreamPress

    /** A torrent without a playable URL: the engine starts it, the row shows its progress (T1). */
    data class StartsTorrent(val stream: TvosStream) : StreamPress
}

/** The press rule of StreamsView.select. [isBusy]: a row is not Idle or the player is up. */
fun streamPress(stream: TvosStream, isBusy: Boolean): StreamPress = when {
    isBusy -> StreamPress.Ignored
    stream.playableUrl == null && stream.infoHash != null -> StreamPress.StartsTorrent(stream)
    stream.playableUrl == null -> StreamPress.OpensSheet(StreamSheet(stream))
    else -> StreamPress.Checks(stream)
}

/** The row for [stream], with the addon text read once. */
fun streamRowItem(stream: TvosStream): StreamRowItem {
    val features = StreamFeatures.parse(
        texts = listOfNotNull(stream.name, stream.title, stream.detail, stream.filename),
        providedBadges = stream.badges,
        providedSize = stream.sizeLabel
    )
    return StreamRowItem(
        stream = stream,
        features = features,
        oneLineName = oneLineName(stream.name),
        tags = features.capabilities.take(MAX_CAPABILITIES) + listOfNotNull(StreamFileFormat.of(stream))
    )
}

/**
 * An addon's multi line name ("Torrentio\n4k") on one line, its parts three spaces apart, as the
 * row and the info sheet show it.
 */
fun oneLineName(name: String): String = name.lines().map(String::trim).filter(String::isNotEmpty).joinToString("   ")

private const val MAX_CAPABILITIES = 4
