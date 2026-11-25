import XCTest
@testable import NuvioTv

final class MpvDynamicRangeTests: XCTestCase {
    func testRangeIsUnknownBeforeTheFirstDecodedFrame() {
        let range = MpvDynamicRange.resolve(decodedGamma: nil, dolbyVisionProfile: nil)

        XCTAssertNil(range)
    }

    func testDolbyVisionTrackIsPerceptualQuantizerBeforeAnyFrame() {
        let range = MpvDynamicRange.resolve(decodedGamma: nil, dolbyVisionProfile: "8")

        XCTAssertEqual(range, .perceptualQuantizer)
    }

    func testFrameWithoutTransferTagIsStandardRange() {
        let range = MpvDynamicRange.resolve(decodedGamma: "auto", dolbyVisionProfile: nil)

        XCTAssertEqual(range, .standard)
    }
}
