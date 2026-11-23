package com.nuvio.tvos.rules

import kotlin.test.Test
import kotlin.test.assertEquals

// Ported from apps/apple-tv/Tests/StreamFeaturesTests.swift: the same cases, so both apps read
// addon text the same way.
class StreamFeaturesTest {
    @Test
    fun aTorrentioRemuxBecomes4kWithSizeSeedersAndSixCapabilities() {
        val texts = listOf(
            "Torrentio\n4k DV | HDR",
            "Movie.2024.2160p.UHD.BluRay.REMUX.DV.HDR10.HEVC.TrueHD.Atmos.7.1-GRP\n" +
                "👤 154 💾 58.3 GB ⚙️ tracker",
        )

        val features = StreamFeatures.parse(texts, providedBadges = emptyList(), providedSize = null)

        assertEquals(
            StreamFeatures(
                resolution = "4K",
                size = "58.3 GB",
                seeders = 154,
                capabilities = listOf("Dolby Vision", "HDR10", "Dolby Atmos", "7.1", "HEVC", "REMUX"),
            ),
            features,
        )
    }

    @Test
    fun aWebDlWithACommaSizeInGibBecomes1080pAndGb() {
        val texts = listOf("Movie.2024.1080p.WEB-DL.DDP5.1.H.264-GRP", "💾 4,2 GiB")

        val features = StreamFeatures.parse(texts, providedBadges = emptyList(), providedSize = null)

        assertEquals(
            StreamFeatures(
                resolution = "1080p",
                size = "4.2 GB",
                seeders = null,
                capabilities = listOf("Dolby Digital+", "5.1", "H.264", "WEB-DL"),
            ),
            features,
        )
    }

    @Test
    fun aSizeIsNotReadAsAudioChannels() {
        val texts = listOf("Direct 720p", "Size 2.0 GB")

        val features = StreamFeatures.parse(texts, providedBadges = emptyList(), providedSize = null)

        assertEquals(
            StreamFeatures(resolution = "720p", size = "2.0 GB", seeders = null, capabilities = emptyList()),
            features,
        )
    }

    @Test
    fun sizeFromTheAddonWinsAndItsResolutionBadgeIsDropped() {
        val features = StreamFeatures.parse(
            texts = listOf("Movie 1080p 💾 4.2 GB"),
            providedBadges = listOf("1080p", "HDR"),
            providedSize = "4.3 GB",
        )

        assertEquals("4.3 GB", features.size)
        assertEquals(listOf("HDR"), features.capabilities)
    }
}
