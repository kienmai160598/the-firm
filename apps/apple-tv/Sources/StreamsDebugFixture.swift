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
        name: "Fixture 1080p",
        title: nil,
        detail: "Sample HLS stream used only to check this layout",
        badges: ["1080p", "HDR"],
        sizeLabel: "2.1 GB",
        kind: "Direct link",
        url: bipbopUrl,
        externalUrl: nil,
        infoHash: nil,
        fileIndex: nil,
        filename: "bipbop.m3u8",
        trackers: [],
        addonId: "debug-fixture",
        addonName: "Debug fixture",
        playableUrl: bipbopUrl,
        requestHeaders: [:]
    )

    private static let torrentStream = TvosStream(
        key: "debug-fixture#1",
        name: "Fixture 4K",
        title: nil,
        detail: "Torrent entry to check the infoHash display",
        badges: ["2160p"],
        sizeLabel: "14.8 GB",
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
