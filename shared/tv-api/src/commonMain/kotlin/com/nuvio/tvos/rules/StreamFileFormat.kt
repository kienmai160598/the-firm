package com.nuvio.tvos.rules

import com.nuvio.tvos.TvosStream

/**
 * The file format tag of a stream row, such as "MKV" or "MP4": the container named by the first
 * known extension in the file name, the addon's title and description, then the link. An HLS or
 * DASH playlist reads "HLS" or "DASH". Null when no text names a format the list knows.
 */
object StreamFileFormat {
    fun of(stream: TvosStream): String? =
        label(listOf(stream.filename, stream.title, stream.detail, stream.playableUrl, stream.url))

    /** [texts] in the order they are trusted; the first one that names a format wins. */
    fun label(texts: List<String?>): String? = texts.firstNotNullOfOrNull { text ->
        text?.let(extensionPattern::find)?.groupValues?.get(1)?.lowercase()?.let(labels::get)
    }

    private val labels = mapOf(
        "mkv" to "MKV",
        "mp4" to "MP4",
        "m4v" to "M4V",
        "mov" to "MOV",
        "avi" to "AVI",
        "webm" to "WebM",
        "wmv" to "WMV",
        "ts" to "TS",
        "m2ts" to "M2TS",
        "iso" to "ISO",
        "m3u8" to "HLS",
        "mpd" to "DASH",
    )

    // An extension ends its word: the end of the text, a space, or a link's query or fragment.
    private val extensionPattern = Regex(
        """\.(${labels.keys.joinToString("|")})(?=$|[\s?#&"')\]])""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )
}
