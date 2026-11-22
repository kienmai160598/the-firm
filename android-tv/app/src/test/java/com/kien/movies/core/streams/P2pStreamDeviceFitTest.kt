package com.kien.movies.core.streams

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class P2pStreamDeviceFitTest {
    private val twoHourFilm = TitleDuration(runtimeMinutes = 120, isEpisode = false)
    private val fortyFiveMinuteEpisode = TitleDuration(runtimeMinutes = 45, isEpisode = true)

    @Test
    fun `a film's bitrate is its size over its runtime`() {
        val stream = torrent(videoSize = 9L * GIBIBYTE)

        assertEquals(9L * GIBIBYTE * 8L / 7200L, p2pStreamBitsPerSecond(stream, twoHourFilm))
    }

    @Test
    fun `a 60 GB remux of a two hour film exceeds the budget`() {
        assertTrue(exceedsP2pBitrateBudget(torrent(videoSize = 60L * GIBIBYTE), twoHourFilm))
    }

    @Test
    fun `a 10 GB encode of a two hour film fits the budget`() {
        assertFalse(exceedsP2pBitrateBudget(torrent(videoSize = 10L * GIBIBYTE), twoHourFilm))
    }

    @Test
    fun `a film's size is read from the stream text when the addon gives no structured size`() {
        val stream = torrent(title = "Film.2020.2160p.REMUX\n👤 12 💾 58.4 GB ⚙️ Source")

        assertTrue(exceedsP2pBitrateBudget(stream, twoHourFilm))
    }

    @Test
    fun `an unknown runtime leaves the stream unmarked`() {
        val stream = torrent(videoSize = 60L * GIBIBYTE)

        assertNull(p2pStreamBitsPerSecond(stream, TitleDuration(runtimeMinutes = null, isEpisode = false)))
        assertFalse(exceedsP2pBitrateBudget(stream, TitleDuration(runtimeMinutes = 0, isEpisode = false)))
    }

    @Test
    fun `an unknown size leaves the stream unmarked`() {
        assertFalse(exceedsP2pBitrateBudget(torrent(title = "Film.2020.2160p.REMUX"), twoHourFilm))
    }

    @Test
    fun `a stream that is not P2P is never marked`() {
        val direct = torrent(videoSize = 60L * GIBIBYTE).copy(infoHash = null, url = "https://example.com/film.mkv")

        assertFalse(exceedsP2pBitrateBudget(direct, twoHourFilm))
    }

    @Test
    fun `a season pack size in the text is not read as one episode`() {
        val seasonPack = torrent(title = "Show.S01.1080p.BluRay\n👤 40 💾 38 GB ⚙️ Source", fileIdx = null)

        assertNull(p2pVideoSizeBytes(seasonPack, isEpisode = true))
        assertFalse(exceedsP2pBitrateBudget(seasonPack, fortyFiveMinuteEpisode))
    }

    @Test
    fun `an episode stream that names its file uses the size in its text`() {
        val episodeFile = torrent(title = "Show.S01\nShow.S01E02.2160p.mkv\n👤 40 💾 12 GB ⚙️ Source", fileIdx = 2)

        assertEquals(12L * GIBIBYTE, p2pVideoSizeBytes(episodeFile, isEpisode = true))
        assertTrue(exceedsP2pBitrateBudget(episodeFile, fortyFiveMinuteEpisode))
    }

    @Test
    fun `an episode's structured size is used without a file index`() {
        val stream = torrent(videoSize = 12L * GIBIBYTE, fileIdx = null)

        assertTrue(exceedsP2pBitrateBudget(stream, fortyFiveMinuteEpisode))
    }

    @Test
    fun `streams over the budget move behind the others and keep the addon's order`() {
        val remux = torrent(name = "remux", videoSize = 60L * GIBIBYTE)
        val encode = torrent(name = "encode", videoSize = 10L * GIBIBYTE)
        val bigger = torrent(name = "bigger", videoSize = 70L * GIBIBYTE)
        val unknown = torrent(name = "unknown")

        val ranked = rankP2pStreamsForDevice(listOf(remux, encode, bigger, unknown), twoHourFilm)

        assertEquals(listOf("encode", "unknown", "remux", "bigger"), ranked.map { it.name })
        assertEquals(listOf(false, false, true, true), ranked.map { it.exceedsDeviceBudget })
    }

    @Test
    fun `ranking again without a runtime clears the mark`() {
        val marked = torrent(videoSize = 60L * GIBIBYTE).copy(exceedsDeviceBudget = true)

        val ranked = rankP2pStreamsForDevice(listOf(marked), TitleDuration(null, isEpisode = false))

        assertFalse(ranked.single().exceedsDeviceBudget)
    }
}
