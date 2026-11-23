package com.kien.movies.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/** One label and value of the Info panel. */
data class InfoRow(val label: String, val value: String)

/** The decoders ExoPlayer initialised, reported by its analytics listener. */
data class DecoderNames(val video: String? = null, val audio: String? = null)

/** The Info panel's "Playback" rows (tvOS MpvMediaInfo.swift): video, audio, decoder, buffer. */
@OptIn(UnstableApi::class)
internal object PlayerMediaInfo {
    private val softwarePrefixes = listOf("c2.android.", "omx.google.", "ffmpeg", "libgav1", "libdav1d", "libvpx")

    fun rows(player: ExoPlayer, decoders: DecoderNames): List<InfoRow> = listOf(
        InfoRow("Video", video(player.videoFormat)),
        InfoRow("Audio", audio(player.audioFormat)),
        InfoRow("Decoder", decoder(decoders.video)),
        InfoRow("Audio decoder", decoder(decoders.audio)),
        InfoRow("Buffered", buffered(player.totalBufferedDuration))
    )

    private fun video(format: Format?): String {
        format ?: return UNKNOWN
        val size = if (format.width > 0 && format.height > 0) "${format.width}×${format.height}" else null
        val bitDepth = format.colorInfo?.lumaBitdepth?.takeIf { it > 8 }?.let { "$it-bit" }
        val range = when (format.colorInfo?.colorTransfer) {
            C.COLOR_TRANSFER_ST2084 -> "HDR10"
            C.COLOR_TRANSFER_HLG -> "HLG"
            else -> null
        }
        return joined(size, PlayerTracks.codecName(format), bitDepth, range)
    }

    private fun audio(format: Format?): String {
        format ?: return UNKNOWN
        return joined(PlayerTracks.codecName(format), format.channelCount.takeIf { it > 0 }?.let(PlayerTracks::channelLabel))
    }

    /** "Hardware (c2.amlogic.hevc.decoder)" or "Software (ffmpeg...)". */
    fun decoder(name: String?): String {
        name ?: return UNKNOWN
        val isSoftware = softwarePrefixes.any { name.lowercase().startsWith(it) }
        return "${if (isSoftware) "Software" else "Hardware"} ($name)"
    }

    private fun buffered(bufferedMs: Long): String = if (bufferedMs <= 0) "None" else "${bufferedMs / MILLIS_PER_SECOND} s ahead"

    private fun joined(vararg parts: String?): String = parts.filterNotNull().filter(String::isNotEmpty).joinToString(", ").ifEmpty { UNKNOWN }

    private const val UNKNOWN = "Unknown"
    private const val MILLIS_PER_SECOND = 1_000
}
