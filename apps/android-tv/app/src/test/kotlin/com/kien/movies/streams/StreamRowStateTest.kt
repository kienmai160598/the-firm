package com.kien.movies.streams

import com.nuvio.tvos.TvosTorrentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamRowStateTest {
    @Test
    fun aPressWhileAnotherRowIsBusyIsIgnored() {
        assertEquals(StreamPress.Ignored, streamPress(stream("a#0"), isBusy = true))
    }

    @Test
    fun s4_aStreamWithoutAPlayableUrlOpensTheInfoSheetWithoutANotice() {
        val external = stream("a#0", playableUrl = null)

        assertEquals(StreamPress.OpensSheet(StreamSheet(external)), streamPress(external, isBusy = false))
    }

    @Test
    fun t1_aTorrentWithoutAPlayableUrlStartsInTheEngine() {
        val torrent = stream("a#0", playableUrl = null, infoHash = "0123456789abcdef0123456789abcdef01234567")

        assertEquals(StreamPress.StartsTorrent(torrent), streamPress(torrent, isBusy = false))
    }

    @Test
    fun t1_theRowReadsStartingTorrentUntilTheEngineFirstReports() {
        assertEquals(StreamRowProgress.startingTorrent, torrentProgress(null))
    }

    @Test
    fun t1_theRowShowsTheEngineStepPeersSpeedAndBuffer() {
        val progress = torrentProgress(torrentStatus(step = "Finding peers", peers = 12, seeds = 3, speed = "1.2 MB/s", buffer = 0.4f))

        assertEquals(StreamRowProgress("Finding peers", "12 peers, 3 seeds, 1.2 MB/s", 0.4f), progress)
    }

    @Test
    fun t1_aStatusWithoutAStepReadsStartingTorrent() {
        val progress = torrentProgress(torrentStatus(step = null, peers = 0, seeds = 0, speed = "0 B/s", buffer = 0f))

        assertEquals(StreamRowProgress.startingTorrent.title, progress.title)
    }

    @Test
    fun aPlayableStreamIsCheckedFirst() {
        val direct = stream("a#0")

        assertEquals(StreamPress.Checks(direct), streamPress(direct, isBusy = false))
    }

    @Test
    fun s6_thePressedRowShowsItsProgressAndTheOthersDim() {
        val selection = StreamSelection("a#1", StreamRowProgress.checking)

        assertEquals(StreamRowProgress.checking, selection.progressFor("a#1"))
        assertNull(selection.progressFor("a#0"))
        assertEquals(listOf(true, false), listOf(selection.dims("a#0"), selection.dims("a#1")))
    }

    @Test
    fun s2_aRowReadsItsNameOnOneLineAndTheFirstOtherLineAsTheFile() {
        val row = streamRowItem(
            stream(
                "a#0",
                name = "Torrentio\n4k DV | HDR",
                detail = "Movie.2024.2160p.REMUX.HEVC-GRP\n👤 154 💾 58.3 GB"
            )
        )

        assertEquals("Torrentio   4k DV | HDR", row.oneLineName)
        assertEquals("Movie.2024.2160p.REMUX.HEVC-GRP", row.fileLine)
        assertEquals("4K", row.features.resolution)
        assertEquals(154, row.features.seeders)
    }

    @Test
    fun theAddonFileNameWinsAsTheFileLine() {
        val row = streamRowItem(stream("a#0", detail = "Some detail", filename = "Movie.mkv"))

        assertEquals("Movie.mkv", row.fileLine)
    }

    private fun torrentStatus(step: String?, peers: Int, seeds: Int, speed: String, buffer: Float) = TvosTorrentStatus(
        phase = "connecting",
        step = step,
        localUrl = null,
        peers = peers,
        seeds = seeds,
        downloadSpeedLabel = speed,
        bufferFraction = buffer,
        downloadedFraction = 0f,
        errorMessage = null
    )
}
