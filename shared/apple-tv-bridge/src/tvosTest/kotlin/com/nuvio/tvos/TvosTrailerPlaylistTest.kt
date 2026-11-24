package com.nuvio.tvos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val AUDIO = """#EXT-X-MEDIA:URI="https://example.test/audio.m3u8",TYPE=AUDIO,GROUP-ID="a",NAME="en""""

private fun variant(
    resolution: String,
    codecs: String,
    uri: String,
) = listOf("""#EXT-X-STREAM-INF:BANDWIDTH=1,CODECS="$codecs",RESOLUTION=$resolution,AUDIO="a"""", uri)

private fun master(vararg variants: List<String>) =
    (listOf("#EXTM3U", "#EXT-X-INDEPENDENT-SEGMENTS", AUDIO) + variants.flatMap { it }).joinToString("\n")

class TvosTrailerPlaylistTest {
    @Test
    fun onlyTheSharpestH264VariantsStayWithTheirRenditions() {
        val playlist = master(
            variant("426x240", "avc1.4D4015,mp4a.40.2", "https://example.test/240.m3u8"),
            variant("1920x1080", "avc1.640028,mp4a.40.2", "https://example.test/1080.m3u8"),
            variant("1280x720", "avc1.4D401F,mp4a.40.2", "https://example.test/720.m3u8"),
        )

        val sharpest = sharpestTierPlaylist(playlist)

        assertEquals(
            master(variant("1920x1080", "avc1.640028,mp4a.40.2", "https://example.test/1080.m3u8")),
            sharpest,
        )
    }

    @Test
    fun vp9VariantsAreSkippedEvenWhenTheyAreSharper() {
        val playlist = master(
            variant("1280x720", "avc1.4D401F,mp4a.40.2", "https://example.test/720.m3u8"),
            variant("3840x2160", "vp09.00.50.08,mp4a.40.2", "https://example.test/2160-vp9.m3u8"),
        )

        val sharpest = sharpestTierPlaylist(playlist).orEmpty()

        assertTrue("https://example.test/720.m3u8" in sharpest)
        assertFalse("2160-vp9" in sharpest)
    }

    @Test
    fun aPlaylistWithoutPlayableVariantsIsLeftToTheOriginal() {
        val playlist = master(variant("1920x1080", "vp09.00.40.08,mp4a.40.2", "https://example.test/1080.m3u8"))

        assertNull(sharpestTierPlaylist(playlist))
    }

    @Test
    fun onlyHlsMasterLinksAreRewritten() {
        assertTrue(isHlsMasterLink("https://manifest.googlevideo.com/api/manifest/hls_variant/expire/1/file/index.m3u8"))
        assertFalse(isHlsMasterLink("https://rr1.googlevideo.com/videoplayback?itag=18&mime=video%2Fmp4"))
    }
}
