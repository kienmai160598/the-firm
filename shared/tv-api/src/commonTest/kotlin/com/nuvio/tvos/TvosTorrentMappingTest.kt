package com.nuvio.tvos

import com.nuvio.app.features.p2p.P2pStreamingState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val INFO_HASH = "0123456789abcdef0123456789abcdef01234567"
private const val TRACKER = "udp://tracker.example:1337/announce"
private const val LOCAL_URL = "http://127.0.0.1:41234/stream/abc"

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

    @Test
    fun aStreamingTorrentExposesItsLocalUrl() {
        val streaming = P2pStreamingState.Streaming(
            localUrl = LOCAL_URL,
            downloadSpeed = 2L * 1_048_576L,
            uploadSpeed = 0L,
            peers = 12,
            seeds = 4,
            bufferProgress = 0.25f,
            totalProgress = 0.5f,
        )

        val status = tvosTorrentStatus(streaming)

        assertEquals(TORRENT_PHASE_STREAMING, status.phase)
        assertEquals(LOCAL_URL, status.localUrl)
        assertEquals("2.0 MB/s", status.downloadSpeedLabel)
        assertEquals(0.25f, status.bufferFraction)
    }

    @Test
    fun aFailedTorrentCarriesItsMessageAndNoUrl() {
        val status = tvosTorrentStatus(P2pStreamingState.Error("No peers"))

        assertEquals(TORRENT_PHASE_FAILED, status.phase)
        assertEquals("No peers", status.errorMessage)
        assertNull(status.localUrl)
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
