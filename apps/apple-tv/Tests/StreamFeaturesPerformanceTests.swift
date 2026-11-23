import XCTest
@testable import NuvioTv

/// Cost of reading the badges of a long torrent list, the work one Streams list pass does per row.
/// Take numbers only from an optimized build on a device.
final class StreamFeaturesPerformanceTests: XCTestCase {
    private static let streamCount = 100
    private static let texts = [
        "Torrentio\n4k DV | HDR",
        "Movie.2024.2160p.UHD.BluRay.REMUX.DV.HDR10.HEVC.TrueHD.Atmos.7.1-GRP\n"
            + "👤 154 💾 58.3 GB ⚙️ tracker",
    ]

    func testReadingAHundredTorrentRows() {
        measure(metrics: [XCTClockMetric()]) {
            for index in 0..<Self.streamCount {
                let features = StreamFeatures(
                    texts: Self.texts + ["\(index)"],
                    providedBadges: [],
                    providedSize: nil
                )
                XCTAssertEqual(features.resolution, "4K")
            }
        }
    }
}
