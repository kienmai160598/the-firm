package com.nuvio.tvos

/**
 * The body of a trailer's HLS master playlist, read with the headers the YouTube extractor uses,
 * or null when the server did not answer with success. Each bridge reads it through its platform's
 * `TrailerExtractionPlatform`, which only the platform source sets can see.
 */
internal expect suspend fun readTrailerPlaylist(url: String): String?
