package com.kien.movies.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.nuvio.tvos.rules.OnlineSubtitle
import java.util.Locale

/** The two track menus (tvOS MpvTrack.swift). */
enum class TrackKind(val menuTitle: String, val trackType: Int) {
    Audio(menuTitle = "Audio Track", trackType = C.TRACK_TYPE_AUDIO),
    Subtitles(menuTitle = "Subtitles", trackType = C.TRACK_TYPE_TEXT)
}

/** One row of a track menu; [online] is set for an online subtitle that is not loaded yet. */
data class TrackOption(
    val id: String,
    val label: String,
    val detail: String?,
    val isSelected: Boolean,
    val online: OnlineSubtitle? = null
)

/** Reads the menu rows from ExoPlayer's tracks and turns a choice into selection parameters. */
@OptIn(UnstableApi::class)
internal object PlayerTracks {
    const val OFF_ID = "off"

    /**
     * The rows of [kind]'s menu: for subtitles "Off", the embedded and loaded tracks, then the
     * [online] subtitles whose id is not in [loadedOnlineIds].
     */
    fun options(kind: TrackKind, tracks: Tracks, online: List<OnlineSubtitle>, loadedOnlineIds: Set<String>): List<TrackOption> {
        val embedded = tracks.groups.withIndex()
            .filter { (_, group) -> group.type == kind.trackType }
            .flatMap { (groupIndex, group) ->
                (0 until group.length).map { trackIndex ->
                    option(id = trackId(groupIndex, trackIndex), format = group.getTrackFormat(trackIndex), number = trackIndex + 1, isSelected = group.isTrackSelected(trackIndex))
                }
            }
        if (kind != TrackKind.Subtitles) return embedded
        val off = TrackOption(id = OFF_ID, label = "Off", detail = null, isSelected = embedded.none(TrackOption::isSelected))
        val notLoaded = online.filter { it.id !in loadedOnlineIds }.map { subtitle ->
            TrackOption(id = subtitle.id, label = subtitle.label, detail = subtitle.detail, isSelected = false, online = subtitle)
        }
        return listOf(off) + embedded + notLoaded
    }

    /** The language codes of the file's subtitle tracks, before any online subtitle is loaded. */
    fun embeddedSubtitleLanguages(tracks: Tracks): List<String> = tracks.groups
        .filter { group -> group.type == C.TRACK_TYPE_TEXT }
        .flatMap { group -> (0 until group.length).mapNotNull { index -> group.getTrackFormat(index).language } }

    /** Parameters that select [optionId] of [kind]; null when the id is not an embedded track. */
    fun selecting(kind: TrackKind, optionId: String, tracks: Tracks, current: TrackSelectionParameters): TrackSelectionParameters? {
        if (optionId == OFF_ID) {
            return current.buildUpon().clearOverridesOfType(kind.trackType).setTrackTypeDisabled(kind.trackType, true).build()
        }
        val (groupIndex, trackIndex) = parseTrackId(optionId) ?: return null
        val group = tracks.groups.getOrNull(groupIndex)?.takeIf { it.type == kind.trackType } ?: return null
        return current.buildUpon()
            .setTrackTypeDisabled(kind.trackType, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
            .build()
    }

    /** Parameters that select the text track whose format id ends with [subtitleId], if it is there yet. */
    fun selectingLoaded(subtitleId: String, tracks: Tracks, current: TrackSelectionParameters): TrackSelectionParameters? {
        tracks.groups.forEach { group ->
            if (group.type != C.TRACK_TYPE_TEXT) return@forEach
            for (trackIndex in 0 until group.length) {
                // Merged sources prefix the sideloaded id ("1:online:..."), so match the end.
                if (group.getTrackFormat(trackIndex).id?.endsWith(subtitleId) == true) {
                    return current.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                        .build()
                }
            }
        }
        return null
    }

    fun channelLabel(count: Int): String = when (count) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> "$count ch"
    }

    /** A short codec name for menus and the info panel ("AC3", "HEVC", "ASS"). */
    fun codecName(format: Format): String? {
        // Subtitles parsed during extraction carry their original type in codecs.
        val mime = if (format.sampleMimeType == MimeTypes.APPLICATION_MEDIA3_CUES) format.codecs else format.sampleMimeType
        return mime?.let { codecNames[it] ?: it.substringAfter('/').removePrefix("x-").uppercase() }
    }

    fun languageName(code: String?): String? {
        if (code.isNullOrBlank() || code == "und") return null
        val name = Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH)
        return name.takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) }?.replaceFirstChar { it.titlecase(Locale.ENGLISH) } ?: code
    }

    private fun option(id: String, format: Format, number: Int, isSelected: Boolean): TrackOption {
        val title = format.label?.takeIf(String::isNotBlank)
        val language = languageName(format.language)
        val channels = format.channelCount.takeIf { it > 0 }?.let(::channelLabel)
        val details = listOfNotNull(if (title == null) null else language, codecName(format), channels)
        return TrackOption(
            id = id,
            label = title ?: language ?: "Track $number",
            detail = details.takeIf { it.isNotEmpty() }?.joinToString(", "),
            isSelected = isSelected
        )
    }

    private fun trackId(groupIndex: Int, trackIndex: Int) = "track:$groupIndex:$trackIndex"

    private fun parseTrackId(id: String): Pair<Int, Int>? {
        val parts = id.split(':')
        if (parts.size != 3 || parts[0] != "track") return null
        val group = parts[1].toIntOrNull() ?: return null
        val track = parts[2].toIntOrNull() ?: return null
        return group to track
    }

    private val codecNames = mapOf(
        MimeTypes.AUDIO_AC3 to "AC3",
        MimeTypes.AUDIO_E_AC3 to "EAC3",
        MimeTypes.AUDIO_E_AC3_JOC to "EAC3 Atmos",
        MimeTypes.AUDIO_AC4 to "AC4",
        MimeTypes.AUDIO_DTS to "DTS",
        MimeTypes.AUDIO_DTS_HD to "DTS-HD",
        MimeTypes.AUDIO_DTS_EXPRESS to "DTS Express",
        MimeTypes.AUDIO_TRUEHD to "TrueHD",
        MimeTypes.AUDIO_AAC to "AAC",
        MimeTypes.AUDIO_MPEG to "MP3",
        MimeTypes.AUDIO_OPUS to "Opus",
        MimeTypes.AUDIO_VORBIS to "Vorbis",
        MimeTypes.AUDIO_FLAC to "FLAC",
        MimeTypes.VIDEO_H264 to "H.264",
        MimeTypes.VIDEO_H265 to "HEVC",
        MimeTypes.VIDEO_AV1 to "AV1",
        MimeTypes.VIDEO_VP9 to "VP9",
        MimeTypes.VIDEO_DOLBY_VISION to "Dolby Vision",
        MimeTypes.VIDEO_MPEG2 to "MPEG-2",
        MimeTypes.TEXT_SSA to "ASS",
        MimeTypes.APPLICATION_SUBRIP to "SRT",
        MimeTypes.TEXT_VTT to "VTT",
        MimeTypes.APPLICATION_PGS to "PGS",
        MimeTypes.APPLICATION_VOBSUB to "VobSub",
        MimeTypes.APPLICATION_TTML to "TTML",
        MimeTypes.APPLICATION_TX3G to "TX3G"
    )
}
