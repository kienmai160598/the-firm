package com.nuvio.tvos.rules

import com.nuvio.tvos.TvosStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LastLinkTest {
    @Test
    fun aTorrentPlayedWithinADayComesBackAsItWasPicked() {
        val payload = LastLink.encode(torrent, savedAtMs = 1_000)

        val recalled = LastLink.decode(payload, nowMs = 1_000 + 23 * HOUR_MS)

        assertEquals(torrent, recalled)
    }

    @Test
    fun aTorrentOlderThanADayIsStillReused() {
        val payload = LastLink.encode(torrent, savedAtMs = 1_000)

        val recalled = LastLink.decode(payload, nowMs = 1_000 + 30 * DAY_MS)

        assertEquals(torrent, recalled)
    }

    @Test
    fun aDirectLinkOlderThanADayIsNotReused() {
        val payload = LastLink.encode(directLink, savedAtMs = 1_000)

        val recalled = LastLink.decode(payload, nowMs = 1_000 + DAY_MS + 1)

        assertNull(recalled)
    }

    private companion object {
        const val HOUR_MS = 60L * 60 * 1000
        const val DAY_MS = 24 * HOUR_MS

        val torrent = TvosStream(
            key = "torrentio#3",
            name = "Torrentio 2160p",
            title = "Film.2026.2160p.mkv",
            detail = null,
            badges = listOf("4K"),
            sizeLabel = "18.2 GB",
            kind = "Torrent",
            url = null,
            externalUrl = null,
            infoHash = "abc123",
            fileIndex = "2",
            filename = "Film.2026.2160p.mkv",
            trackers = listOf("udp://tracker.test:80"),
            addonId = "torrentio",
            addonName = "Torrentio",
            playableUrl = null,
            requestHeaders = emptyMap(),
        )

        val directLink = torrent.copy(
            key = "debrid#1",
            kind = "Direct",
            infoHash = null,
            playableUrl = "https://debrid.test/film.mkv",
        )
    }
}
