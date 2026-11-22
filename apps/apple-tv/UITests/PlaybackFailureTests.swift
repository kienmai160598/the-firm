import XCTest

final class PlaybackFailureTests: XCTestCase {
    private static let tearsOfSteel = "details/movie/tt2285752"
    private static let tearsOfSteelStreams = "streams/movie/tt2285752"
    private static let failureTitle = "Playback failed"
    private static let playerError = "The player stopped with an error"
    private static let codecStreams = [
        "Codec H.264 control", "Codec MPEG-2 video", "Codec MPEG-1 video",
    ]

    private var tv: TvApp!

    override func setUpWithError() throws {
        continueAfterFailure = false
        try XCTSkipUnless(TvApp.isAddonServerUp, "Start serve_test_addon.py on port 8765 first")
        tv = TvApp(self)
    }

    func testCodecSamplesPlayWithoutNoticeOrFailure() {
        tv.launch(route: "streams/movie/tvostest-codecs")
        let notice = tv.app.staticTexts["Apple TV cannot play this stream"]
        let failure = tv.app.staticTexts[Self.failureTitle]

        for name in Self.codecStreams {
            let stream = tv.button(containing: name)
            XCTAssertTrue(stream.waitForExistence(timeout: 60), name)

            tv.select(stream)

            XCTAssertFalse(notice.waitForExistence(timeout: 4), "\(name) showed a notice")
            XCTAssertFalse(failure.waitForExistence(timeout: 4), "\(name) failed")
            XCTAssertTrue(tv.pressMenu(untilFocused: stream), name)
        }
    }

    func testDropoutFailsMidPlaybackAndKeepsProgress() {
        tv.launch(route: "streams/movie/tvostest-dropout")
        let stream = tv.button(containing: "Test Dropout MP4")
        XCTAssertTrue(stream.waitForExistence(timeout: 60))

        tv.select(stream)

        XCTAssertTrue(tv.app.staticTexts[Self.failureTitle].waitForExistence(timeout: 120))
        XCTAssertTrue(tv.text(beginningWith: Self.playerError).exists)
        tv.screenshot("failure-dropout")
        XCTAssertTrue(tv.pressMenu(until: tv.button(beginningWith: "Resume")))
        XCTAssertTrue(tv.text(beginningWith: "Resume from").waitForExistence(timeout: 5))
    }

    func testStalledLinkIsCheckedInTheHeaderThenFailsAndSavesNothing() {
        tv.launch(route: Self.tearsOfSteelStreams)
        let stalled = tv.button(containing: "Test Stalled link")
        let checking = tv.app.staticTexts["Checking stream"]
        XCTAssertTrue(stalled.waitForExistence(timeout: 60))

        tv.select(stalled)

        XCTAssertTrue(checking.waitForExistence(timeout: 3))
        XCTAssertLessThan(checking.frame.maxY, stalled.frame.minY)
        XCTAssertTrue(tv.app.staticTexts[Self.failureTitle].waitForExistence(timeout: 75))
        XCTAssertTrue(tv.text(beginningWith: Self.playerError).exists)
        tv.launch(route: Self.tearsOfSteel, keepingState: true)
        XCTAssertTrue(tv.button(beginningWith: "Play").waitForExistence(timeout: 60))
        XCTAssertFalse(tv.button(beginningWith: "Resume").exists)
    }

    func testStreamWithoutExtensionIsProbedAndFails() {
        tv.launch(route: Self.tearsOfSteelStreams)
        let garbage = tv.button(containing: "Test Garbage no extension")
        XCTAssertTrue(garbage.waitForExistence(timeout: 60))

        tv.select(garbage)

        XCTAssertTrue(tv.app.staticTexts[Self.failureTitle].waitForExistence(timeout: 30))
        XCTAssertTrue(tv.text(beginningWith: Self.playerError).exists)
    }
}
