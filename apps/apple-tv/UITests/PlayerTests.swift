import XCTest

final class PlayerTests: XCTestCase {
    private static let tearsOfSteelStreams = "streams/movie/tt2285752"
    private static let bigBuckBunny = "details/movie/tt1254207"

    private var tv: TvApp!

    override func setUpWithError() throws {
        continueAfterFailure = false
        try XCTSkipUnless(TvApp.isAddonServerUp, "Start serve_test_addon.py on port 8765 first")
        tv = TvApp(self)
    }

    func testMkvControlsSkipTenSecondsAndHideWhilePlaying() {
        openMkv()
        let player = tv.app.buttons["mpv-player"]

        tv.remote.press(.down)
        tv.remote.press(.right)

        XCTAssertTrue(tv.appearsBriefly(tv.app.images["Skipped forward"], within: 1))
        XCTAssertFalse(tv.app.images["Paused"].exists, "Pause icon shown while playing")
        tv.screenshot("player-skip-forward")
        XCTAssertTrue(tv.waitForDisappearance(tv.app.buttons["Info"], timeout: 8))
        XCTAssertTrue(player.hasFocus)
    }

    func testStartShowsGenresAndTimesOnlyOnceDurationIsKnown() {
        openMkv()
        let advisory = tv.app.descendants(matching: .any)["playback-advisory"]
        let remaining = tv.text(beginningWith: "-")

        XCTAssertTrue(advisory.waitForExistence(timeout: 15))
        XCTAssertTrue(remaining.exists)
        tv.screenshot("player-start-advisory")
        XCTAssertTrue(tv.waitForDisappearance(advisory, timeout: 10))
    }

    func testSubtitleMenuLoadsAnOnlineVietnameseSubtitle() {
        openMkv()
        let embedded = tv.button(containing: "Vietnamese ASS")
        let online = onlineButton(language: "Vietnamese")
        let loaded = tv.button(containing: "Vietnamese · Nuvio tvOS test films")
        openMenu("Subtitles")
        XCTAssertTrue(embedded.waitForExistence(timeout: 3))
        tv.focus(online, moving: [.down, .down, .down, .down])
        tv.screenshot("player-subtitles-online-listed")

        tv.remote.press(.select)

        XCTAssertTrue(tv.waitForFocus(tv.app.buttons["Subtitles"]))
        tv.remote.press(.select)
        XCTAssertTrue(loaded.waitForExistence(timeout: 5))
        XCTAssertFalse(online.exists)
        tv.screenshot("player-subtitles-online-loaded")
    }

    func testAudioMenuReachesEveryTrackAndInfoShowsDecoder() {
        openMkv()
        let dts = tv.button(containing: "Vietnamese DTS")
        openMenu("Audio")
        XCTAssertTrue(dts.waitForExistence(timeout: 3))

        tv.focus(dts, moving: [.down, .down, .down, .down])
        tv.remote.press(.select)
        XCTAssertTrue(tv.waitForFocus(tv.app.buttons["Audio"]))
        tv.remote.press(.right)
        tv.remote.press(.select)

        XCTAssertTrue(tv.app.staticTexts["Decoder"].waitForExistence(timeout: 3))
        tv.screenshot("player-info")
    }

    func testHttpStreamSavesProgressAndRelaunchResumesThere() {
        tv.launch(route: Self.bigBuckBunny)
        let stream = tv.button(containing: "Test Pattern MP4")
        tv.select(tv.button(beginningWith: "Play"))
        XCTAssertTrue(stream.waitForExistence(timeout: 30))
        tv.select(stream)
        let advisory = tv.app.descendants(matching: .any)["playback-advisory"]
        XCTAssertTrue(advisory.waitForExistence(timeout: 15))
        tv.screenshot("player-avkit-start-advisory")
        tv.watch(seconds: 6)
        tv.pressMenu(untilFocused: stream)
        XCTAssertTrue(tv.pressMenu(until: tv.button(beginningWith: "Resume")))
        let savedLabel = tv.text(beginningWith: "Resume from").label
        tv.launch(route: Self.bigBuckBunny, keepingState: true)
        tv.select(tv.button(beginningWith: "Resume"))
        XCTAssertTrue(stream.waitForExistence(timeout: 30))

        tv.select(stream)

        let resumeChoice = tv.app.buttons[savedLabel].firstMatch
        XCTAssertTrue(resumeChoice.waitForExistence(timeout: 15))
        XCTAssertTrue(tv.app.buttons["Start from beginning"].exists)
        tv.screenshot("player-resume-choice")
    }

    func testTorrentStreamPlaysThroughTheEngine() {
        tv.launch(route: "streams/movie/tt1727587")
        let torrent = tv.button(containing: "Test Torrent")
        XCTAssertTrue(torrent.waitForExistence(timeout: 60))

        tv.select(torrent)

        let failed = tv.app.staticTexts["Playback failed"]
        let torrentNote = tv.text(beginningWith: "This is a torrent")
        XCTAssertFalse(failed.waitForExistence(timeout: 20), "Torrent playback failed")
        XCTAssertFalse(torrentNote.exists, "Torrent opened the info sheet")
        tv.screenshot("player-torrent")
    }

    private func openMkv() {
        tv.launch(route: Self.tearsOfSteelStreams)
        let mkv = tv.button(containing: "Test MKV")
        XCTAssertTrue(mkv.waitForExistence(timeout: 60))
        tv.select(mkv)
        XCTAssertTrue(tv.app.buttons["mpv-player"].waitForExistence(timeout: 20))
        XCTAssertTrue(tv.waitForFocus(tv.app.buttons["mpv-player"], timeout: 10))
    }

    private func openMenu(_ control: String) {
        tv.remote.press(.up)
        XCTAssertTrue(tv.waitForFocus(tv.app.buttons["Subtitles"]))
        tv.focus(tv.app.buttons[control], moving: [.right, .right])
        tv.remote.press(.select)
    }

    private func onlineButton(language: String) -> XCUIElement {
        let predicate = NSPredicate(
            format: "label CONTAINS %@ AND label CONTAINS %@", language, "Online"
        )
        return tv.app.buttons.matching(predicate).firstMatch
    }
}
