#if DEBUG
import NuvioTvShared

enum StreamsDebugFixture {
    private static let bipbopUrl = "https://devstreaming-cdn.apple.com/videos/streaming/"
        + "examples/img_bipbop_adv_example_fmp4/master.m3u8"

    static let group = TvosStreamGroup(
        addonId: "debug-fixture",
        addonName: "Debug fixture (not a real addon)",
        streams: [directStream, torrentStream],
        isLoading: false,
        errorMessage: nil
    )

    private static let directStream = TvosStream(
        key: "debug-fixture#0",
        name: "Fixture Debrid\n1080p",
        title: nil,
        detail: "Fixture.2024.1080p.WEB-DL.DDP5.1.Atmos.H.264-GRP\n💾 4.2 GB",
        badges: [],
        sizeLabel: nil,
        kind: "Direct link",
        url: bipbopUrl,
        externalUrl: nil,
        infoHash: nil,
        fileIndex: nil,
        filename: nil,
        trackers: [],
        addonId: "debug-fixture",
        addonName: "Debug fixture",
        playableUrl: bipbopUrl,
        requestHeaders: [:]
    )

    private static let torrentStream = TvosStream(
        key: "debug-fixture#1",
        name: "Fixture Torrent\n4k DV | HDR",
        title: nil,
        detail: "Fixture.2024.2160p.UHD.BluRay.REMUX.DV.HDR10.HEVC.TrueHD.Atmos.7.1-GRP\n"
            + "👤 154 💾 58.3 GB ⚙️ Fixture tracker",
        badges: [],
        sizeLabel: nil,
        kind: "Torrent",
        url: nil,
        externalUrl: nil,
        infoHash: "0123456789abcdef0123456789abcdef01234567",
        fileIndex: "0",
        filename: nil,
        trackers: [],
        addonId: "debug-fixture",
        addonName: "Debug fixture",
        playableUrl: nil,
        requestHeaders: [:]
    )
}
#endif
