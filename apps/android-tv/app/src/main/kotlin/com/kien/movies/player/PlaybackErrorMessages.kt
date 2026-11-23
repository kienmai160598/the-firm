package com.kien.movies.player

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException

/**
 * Plain words for an ExoPlayer failure, shown in the stream's notice after "The player stopped
 * with an error:". Replaces core's PlaybackErrorMessages.kt, which the Android bridge excludes
 * (it needs the mpv error log); same wording, English only like the rest of the app.
 */
internal object PlaybackErrorMessages {
    private val genericMessages = setOf("playback error", "source error", "unexpected runtime error")

    @OptIn(UnstableApi::class)
    fun reason(error: PlaybackException): String {
        error.findCause<HttpDataSource.InvalidResponseCodeException>()?.let { response ->
            return httpReason(response.responseCode, response.responseMessage, error.errorCodeName)
        }
        if (error.findCause<UnrecognizedInputFormatException>() != null) {
            return "The stream source returned invalid or unplayable content. The link may have " +
                "expired or the server returned an error page instead of video. [${error.errorCodeName}]"
        }
        error.findCause<MediaCodecRenderer.DecoderInitializationException>()?.let { decoder ->
            val detail = decoder.message?.trim()?.takeIf(String::isNotEmpty) ?: decoder.diagnosticInfo?.trim()?.takeIf(String::isNotEmpty)
            return detail?.let { "$it [${error.errorCodeName}]" } ?: error.errorCodeName
        }
        val detail = error.mostRelevantMessage()
        return detail?.let { "$it [${error.errorCodeName}]" } ?: error.errorCodeName
    }

    /** "HTTP 404 Not Found [ERROR_CODE_IO_BAD_HTTP_STATUS]" and a hint for the viewer. */
    fun httpReason(code: Int, statusText: String?, errorCodeName: String): String = buildString {
        append("HTTP $code")
        statusText?.takeIf(String::isNotBlank)?.let { append(" $it") }
        append(" [$errorCodeName]")
        httpHint(code)?.let { append(". ").append(it) }
    }

    /** A plain reason for an HTTP status (NuvioTV's wording). */
    fun httpHint(code: Int): String? = when (code) {
        401, 410 -> "The stream link has expired. Try a different source."
        404 -> "The stream link has expired or been removed. Try a different source."
        429 -> "Too many requests to the stream source. Wait a moment and try again."
        in 500..599 -> "The stream server is currently unavailable. Try a different source."
        in 400..499 -> "The stream source is blocked or restricted. Try a different source."
        else -> null
    }

    private inline fun <reified T : Throwable> Throwable.findCause(): T? = generateSequence(this) { it.cause }.take(MAX_CAUSE_DEPTH).filterIsInstance<T>().firstOrNull()

    private fun Throwable.mostRelevantMessage(): String? = generateSequence(this) { it.cause }
        .take(MAX_CAUSE_DEPTH)
        .mapNotNull { it.message?.trim() }
        .firstOrNull { it.isNotEmpty() && it.lowercase() !in genericMessages }

    private const val MAX_CAUSE_DEPTH = 12
}
