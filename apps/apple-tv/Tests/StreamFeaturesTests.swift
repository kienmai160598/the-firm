import XCTest
@testable import NuvioTv

final class StreamFeaturesTests: XCTestCase {
    private struct Case {
        let texts: [String]
        let resolution: String?
        let size: String?
        let seeders: Int?
        let capabilities: [String]
    }

    private static let cases = [
        Case(
            texts: [
                "Torrentio\n4k DV | HDR",
                "Movie.2024.2160p.UHD.BluRay.REMUX.DV.HDR10.HEVC.TrueHD.Atmos.7.1-GRP\n"
                    + "👤 154 💾 58.3 GB ⚙️ tracker",
            ],
            resolution: "4K",
            size: "58.3 GB",
            seeders: 154,
            capabilities: ["Dolby Vision", "HDR10", "Dolby Atmos", "7.1", "HEVC", "REMUX"]
        ),
        Case(
            texts: ["Movie.2024.1080p.WEB-DL.DDP5.1.H.264-GRP", "💾 4,2 GiB"],
            resolution: "1080p",
            size: "4.2 GB",
            seeders: nil,
            capabilities: ["Dolby Digital+", "5.1", "H.264", "WEB-DL"]
        ),
        Case(
            texts: ["Direct 720p", "Size 2.0 GB"],
            resolution: "720p",
            size: "2.0 GB",
            seeders: nil,
            capabilities: []
        ),
    ]

    func testAddonTextsBecomeResolutionSizeSeedersAndCapabilities() {
        for testCase in Self.cases {
            let features = StreamFeatures(
                texts: testCase.texts,
                providedBadges: [],
                providedSize: nil
            )

            XCTAssertEqual(features.resolution, testCase.resolution, "\(testCase.texts)")
            XCTAssertEqual(features.size, testCase.size, "\(testCase.texts)")
            XCTAssertEqual(features.seeders, testCase.seeders, "\(testCase.texts)")
            XCTAssertEqual(features.capabilities, testCase.capabilities, "\(testCase.texts)")
        }
    }

    func testSizeFromTheAddonWinsOverSizeInTheText() {
        let features = StreamFeatures(
            texts: ["Movie 1080p 💾 4.2 GB"],
            providedBadges: ["1080p", "HDR"],
            providedSize: "4.3 GB"
        )

        XCTAssertEqual(features.size, "4.3 GB")
        XCTAssertEqual(features.capabilities, ["HDR"])
    }
}
