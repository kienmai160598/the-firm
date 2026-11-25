package com.nuvio.tvos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val INFO_HASH = "0123456789abcdef0123456789abcdef01234567"
private const val TRACKER = "udp://tracker.example:1337/announce"

class TvosTorrentMappingTest {
    @Test
    fun aTorrentStreamBecomesARequestWithItsFileIndexAndTrackers() {
        val request = torrentStream(infoHash = INFO_HASH, fileIndex = "3").torrentRequest()

        assertEquals(INFO_HASH, request?.infoHash)
        assertEquals(3, request?.fileIdx)
        assertEquals(listOf(TRACKER), request?.trackers)
    }

    @Test
    fun aStreamWithoutAnInfoHashIsNotATorrent() {
        assertNull(torrentStream(infoHash = " ", fileIndex = null).torrentRequest())
    }

    @Test
    fun aFileIndexThatIsNotANumberLetsTheEnginePickTheFile() {
        val request = torrentStream(infoHash = INFO_HASH, fileIndex = "main").torrentRequest()

        assertNull(request?.fileIdx)
    }

    private fun torrentStream(infoHash: String, fileIndex: String?): TvosStream =
        TvosStream(
            key = "torrent#0",
            name = "Torrent",
            title = null,
            detail = null,
            badges = emptyList(),
            sizeLabel = null,
            kind = "Torrent",
            url = null,
            externalUrl = null,
            infoHash = infoHash,
            fileIndex = fileIndex,
            filename = "movie.mkv",
            trackers = listOf(TRACKER),
            addonId = "addon",
            addonName = "Addon",
            playableUrl = null,
            requestHeaders = emptyMap(),
        )
}
