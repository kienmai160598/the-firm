package com.nuvio.tvos

private const val STREAM_INF = "#EXT-X-STREAM-INF:"
private val RESOLUTION_HEIGHT = Regex("""RESOLUTION=\d+x(\d+)""")
private val CODECS = Regex("""CODECS="([^"]*)"""")

/** Video codecs AVPlayer plays from HLS on every Apple TV this app runs on; YouTube's VP9 is not one. */
private val PLAYABLE_VIDEO_CODECS = listOf("avc1", "hvc1", "hev1")

/** YouTube's HLS master playlists, the only trailer links worth rewriting. */
internal fun isHlsMasterLink(url: String): Boolean =
    "/api/manifest/hls_" in url || url.substringBefore('?').endsWith(".m3u8")

/**
 * Keeps only the sharpest playable variants of an HLS master playlist. YouTube lists 240p first and
 * AVPlayer starts on the first variant, so a trailer looked soft for its first seconds; with one
 * resolution left it starts sharp. Every other line (renditions for the audio, captions, tags)
 * stays as it is, so the audio the variants point at still plays. Returns null when no variant
 * has a playable codec and a resolution, so the caller plays the original playlist.
 */
internal fun sharpestTierPlaylist(master: String): String? {
    val lines = master.lines()
    val heights = lines.indices
        .filter { lines[it].startsWith(STREAM_INF) && it + 1 < lines.size }
        .associateWith { playableHeight(lines[it]) }
    val top = heights.values.filterNotNull().maxOrNull() ?: return null
    val dropped = heights.filterValues { it != top }.keys.flatMap { listOf(it, it + 1) }.toSet()
    return lines.filterIndexed { index, _ -> index !in dropped }.joinToString("\n")
}

private fun playableHeight(streamInf: String): Int? {
    val codecs = CODECS.find(streamInf)?.groupValues?.get(1) ?: return null
    if (PLAYABLE_VIDEO_CODECS.none { codecs.startsWith(it) || ",$it" in codecs }) return null
    return RESOLUTION_HEIGHT.find(streamInf)?.groupValues?.get(1)?.toIntOrNull()
}
