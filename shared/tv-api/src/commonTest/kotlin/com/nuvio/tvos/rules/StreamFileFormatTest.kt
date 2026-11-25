package com.nuvio.tvos.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class StreamFileFormatTest {
    @Test
    fun theFirstTextThatNamesAKnownExtensionGivesTheTag() {
        val cases = mapOf(
            listOf("Movie.mkv", "Movie.2024.2160p.mp4") to "MKV",
            listOf(null, "Movie.2024.2160p.REMUX-GRP.mp4\n👤 154 💾 58.3 GB") to "MP4",
            listOf("Torrentio 4k", "https://cdn.example/v/a.MKV?token=1") to "MKV",
            listOf("https://cdn.example/master.m3u8") to "HLS",
            listOf("Movie.2024.mp4.part.GRP", "https://cdn.example/play?id=7") to null,
        )

        val tags = cases.keys.map(StreamFileFormat::label)

        assertEquals(cases.values.toList(), tags)
    }
}
