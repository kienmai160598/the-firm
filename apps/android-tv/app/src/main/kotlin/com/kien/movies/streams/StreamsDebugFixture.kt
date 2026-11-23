package com.kien.movies.streams

import com.nuvio.tvos.TvosStream
import com.nuvio.tvos.TvosStreamGroup

/**
 * Two known streams for UI tests and manual checks (tvOS StreamsDebugFixture.swift), shown first
 * when the app is launched with `MoviesDebugStreamFixture` (DebugLaunch.usesStreamFixture; always
 * off in the `box` build).
 */
object StreamsDebugFixture {
    private const val BIPBOP_URL = "https://devstreaming-cdn.apple.com/videos/streaming/" +
        "examples/img_bipbop_adv_example_fmp4/master.m3u8"
    private const val ADDON_ID = "debug-fixture"

    const val DIRECT_KEY = "debug-fixture#0"
    const val TORRENT_KEY = "debug-fixture#1"

    private val directStream = TvosStream(
        key = DIRECT_KEY,
        name = "Fixture Debrid\n1080p",
        title = null,
        detail = "Fixture.2024.1080p.WEB-DL.DDP5.1.Atmos.H.264-GRP\n💾 4.2 GB",
        badges = emptyList(),
        sizeLabel = null,
        kind = "Direct link",
        url = BIPBOP_URL,
        externalUrl = null,
        infoHash = null,
        fileIndex = null,
        filename = null,
        trackers = emptyList(),
        addonId = ADDON_ID,
        addonName = "Debug fixture",
        playableUrl = BIPBOP_URL,
        requestHeaders = emptyMap()
    )

    private val torrentStream = TvosStream(
        key = TORRENT_KEY,
        name = "Fixture Torrent\n4k DV | HDR",
        title = null,
        detail = "Fixture.2024.2160p.UHD.BluRay.REMUX.DV.HDR10.HEVC.TrueHD.Atmos.7.1-GRP\n" +
            "👤 154 💾 58.3 GB ⚙️ Fixture tracker",
        badges = emptyList(),
        sizeLabel = null,
        kind = "Torrent",
        url = null,
        externalUrl = null,
        infoHash = "0123456789abcdef0123456789abcdef01234567",
        fileIndex = "0",
        filename = null,
        trackers = emptyList(),
        addonId = ADDON_ID,
        addonName = "Debug fixture",
        playableUrl = null,
        requestHeaders = emptyMap()
    )

    val group = TvosStreamGroup(
        addonId = ADDON_ID,
        addonName = "Debug fixture (not a real addon)",
        streams = listOf(directStream, torrentStream),
        isLoading = false,
        errorMessage = null
    )
}
