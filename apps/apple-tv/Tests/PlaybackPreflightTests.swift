import XCTest
@testable import NuvioTv

final class PlaybackPreflightTests: XCTestCase {
    func testMp4AndMovAreProbedBecauseAVPlayerCanPlayTheirSoundOverABlackPicture() {
        let routes = [["mp4"], ["MOV"], ["", "m4v"]].map(PlaybackPreflight.route(extensions:))

        XCTAssertEqual(routes, [.probe, .probe, .probe])
    }

    func testTheFirstKnownExtensionDecides() {
        let routes = [["m3u8"], ["mkv"], ["php", "mkv"], ["php"]].map(PlaybackPreflight.route(extensions:))

        XCTAssertEqual(routes, [.system, .mpv, .mpv, .probe])
    }
}
