package com.nuvio.tv.core.streams

import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamBehaviorHints
import com.nuvio.tv.domain.model.Video
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val GIBIBYTE = 1024L * 1024L * 1024L

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

    @Test
    fun `an episode's own runtime wins over the show's`() {
        val meta = meta(runtime = "45 min", episodes = listOf(episode(1, 2, runtime = 61)))

        assertEquals(61, titleRuntimeMinutes(meta, season = 1, episode = 2))
    }

    @Test
    fun `an episode without a runtime falls back to the show's`() {
        val meta = meta(runtime = "45 min", episodes = listOf(episode(1, 2, runtime = null)))

        assertEquals(45, titleRuntimeMinutes(meta, season = 1, episode = 2))
    }

    @Test
    fun `a film's runtime is parsed from the meta and missing stays unknown`() {
        assertEquals(148, titleRuntimeMinutes(meta(runtime = "2h 28min"), season = null, episode = null))
        assertNull(titleRuntimeMinutes(meta(runtime = null), season = null, episode = null))
    }

    @Test
    fun `the meta id drops the season and episode of an episode's video id`() {
        assertEquals("tt0903747", metaIdOfVideo("tt0903747:1:2", season = 1, episode = 2))
        assertEquals("kitsu:42", metaIdOfVideo("kitsu:42:1:7", season = 1, episode = 7))
        assertEquals("tt0111161", metaIdOfVideo("tt0111161", season = null, episode = null))
    }

    private fun meta(runtime: String?, episodes: List<Video> = emptyList()): Meta = mockk {
        every { this@mockk.runtime } returns runtime
        every { videos } returns episodes
    }

    private fun episode(season: Int, episode: Int, runtime: Int?): Video = mockk {
        every { this@mockk.season } returns season
        every { this@mockk.episode } returns episode
        every { this@mockk.runtime } returns runtime
    }

    private fun torrent(
        name: String? = null,
        title: String? = null,
        videoSize: Long? = null,
        fileIdx: Int? = 0
    ): Stream = Stream(
        name = name,
        title = title,
        description = null,
        url = null,
        ytId = null,
        infoHash = "0123456789abcdef0123456789abcdef01234567",
        fileIdx = fileIdx,
        externalUrl = null,
        behaviorHints = StreamBehaviorHints(
            notWebReady = null,
            bingeGroup = null,
            countryWhitelist = null,
            proxyHeaders = null,
            videoSize = videoSize
        ),
        addonName = "Addon",
        addonLogo = null
    )
}
