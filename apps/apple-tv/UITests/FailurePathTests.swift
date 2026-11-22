import XCTest

final class FailurePathTests: XCTestCase {
    private static let addonUrl = "http://127.0.0.1:8765/manifest.json"
    private static let codecMovie = "details/movie/tvostest-codecs"
    private static let dropoutMovie = "details/movie/tvostest-dropout"
    private static let tearsOfSteel = "details/movie/tt2285752"
    private static let tearsOfSteelStreams = "streams/movie/tt2285752"
    private static let sentNoData = "The stream sent no data for 20 seconds"
    private static let noticeTitle = "Apple TV cannot play this stream"
    private static let failureTitle = "Playback failed"
    private static let unsupportedFormat = "The player does not support this stream"
    private static let knownMessages = [
        unsupportedFormat, "The stream could not be opened", "The player stopped with an error",
    ]
    private static let codecExpectations = [
        ("Codec H.264 control", "played", "s5-1-codec-h264-plays"),
        ("Codec MPEG-2 video", "notice: \(unsupportedFormat)", "s5-2-codec-mpeg2-notice"),
        ("Codec MPEG-1 video", "notice: \(unsupportedFormat)", "s5-3-codec-mpeg1-notice"),
    ]

    private let app = XCUIApplication()
    private let remote = XCUIRemote.shared

    override func setUpWithError() throws {
        continueAfterFailure = false
        try XCTSkipUnless(Self.isAddonServerUp, "Start serve_test_addon.py on port 8765 first")
    }

    func testCodecSamplesAreProbedBeforePlaying() {
        launch(route: Self.codecMovie)
        let play = primaryButton(beginningWith: "Play")
        XCTAssertTrue(play.waitForExistence(timeout: 60))
        focus(play)
        remote.press(.select)

        for (name, expected, screenshot) in Self.codecExpectations {
            let actual = outcome(selecting: name, screenshot: screenshot)
            XCTAssertTrue(actual.hasPrefix(expected), "\(name): \(actual)")
        }

        pressMenu(until: play)
        XCTAssertTrue(play.waitForExistence(timeout: 10))
        XCTAssertFalse(primaryButton(beginningWith: "Resume").exists)
        saveScreenshot("s5-4-codecs-details-no-resume")
    }

    func testDropoutFailsMidPlaybackAndKeepsProgress() {
        launch(route: Self.dropoutMovie)
        let primary = playOrResumeButton()
        XCTAssertTrue(primary.waitForExistence(timeout: 60))
        focus(primary)
        remote.press(.select)

        let stream = streamButton("Test Dropout MP4")
        XCTAssertTrue(stream.waitForExistence(timeout: 30))
        focus(stream)
        let selectedAt = Date()
        remote.press(.select)
        startOverWhenOffered()
        sleep(15)
        remote.press(.up)
        sleep(1)
        saveScreenshot("s5-5-dropout-playing")

        let failure = app.staticTexts[Self.failureTitle]
        XCTAssertTrue(failure.waitForExistence(timeout: 120))
        print("Dropout failed after \(Date().timeIntervalSince(selectedAt)) s")
        XCTAssertTrue(staticText(beginningWith: "The player stopped with an error").exists)
        waitUntilSheetSettles()
        saveScreenshot("s5-6-dropout-failure")
        remote.press(.menu)

        let resume = primaryButton(beginningWith: "Resume")
        pressMenu(until: resume)
        XCTAssertTrue(staticText(beginningWith: "Resume from").waitForExistence(timeout: 5))
        saveScreenshot("s5-7-dropout-details-resume")
    }

    func testStalledLinkFailsAtStartupDeadlineAndSavesNothing() {
        launch(route: Self.tearsOfSteelStreams)
        let stalled = streamButton("Test Stalled link")
        XCTAssertTrue(stalled.waitForExistence(timeout: 60))
        focus(stalled)
        let selectedAt = Date()
        remote.press(.select)

        let failure = app.staticTexts[Self.failureTitle]
        XCTAssertTrue(failure.waitForExistence(timeout: 70))
        print("Stalled link failed after \(Date().timeIntervalSince(selectedAt)) s")
        XCTAssertTrue(staticText(beginningWith: "The player stopped with an error").exists)
        XCTAssertTrue(app.staticTexts.matching(
            NSPredicate(format: "label CONTAINS %@", Self.sentNoData)
        ).firstMatch.exists)
        waitUntilSheetSettles()
        saveScreenshot("s6-1-stalled-startup-deadline")
        remote.press(.menu)
        XCTAssertTrue(stalled.waitForExistence(timeout: 10))

        launch(route: Self.tearsOfSteel)
        let play = primaryButton(beginningWith: "Play")
        XCTAssertTrue(play.waitForExistence(timeout: 60))
        XCTAssertFalse(primaryButton(beginningWith: "Resume").exists)
        saveScreenshot("s6-2-stalled-details-no-resume")
    }

    private func outcome(selecting name: String, screenshot: String) -> String {
        let stream = streamButton(name)
        XCTAssertTrue(stream.waitForExistence(timeout: 30))
        focus(stream)
        remote.press(.select)
        let notice = app.staticTexts[Self.noticeTitle]
        let failure = app.staticTexts[Self.failureTitle]
        if notice.waitForExistence(timeout: 10) {
            saveScreenshot(screenshot)
            let message = noticeMessage()
            remote.press(.menu)
            XCTAssertTrue(stream.waitForExistence(timeout: 10))
            return "notice: \(message)"
        }
        if failure.waitForExistence(timeout: 8) {
            saveScreenshot(screenshot)
            let message = noticeMessage()
            remote.press(.menu)
            XCTAssertTrue(stream.waitForExistence(timeout: 10))
            return "player failed: \(message)"
        }
        remote.press(.up)
        sleep(1)
        saveScreenshot(screenshot)
        dismissPlayer(returningTo: stream)
        return "played"
    }

    private func playOrResumeButton() -> XCUIElement {
        let startsPlayback = NSPredicate(
            format: "label BEGINSWITH %@ OR label BEGINSWITH %@", "Play", "Resume"
        )
        return app.buttons.matching(startsPlayback).firstMatch
    }

    private func startOverWhenOffered() {
        let startOver = app.buttons["Start from beginning"].firstMatch
        guard startOver.waitForExistence(timeout: 5) else { return }
        focus(startOver)
        remote.press(.select)
    }

    private func waitUntilSheetSettles() {
        let close = app.buttons["Close"].firstMatch
        let hasFocus = NSPredicate(format: "hasFocus == true")
        let focused = expectation(for: hasFocus, evaluatedWith: close)
        XCTAssertEqual(XCTWaiter.wait(for: [focused], timeout: 10), .completed)
        sleep(1)
    }

    private func noticeMessage() -> String {
        Self.knownMessages.first { prefix in
            staticText(beginningWith: prefix).waitForExistence(timeout: 2)
        } ?? "unknown message"
    }

    private static var isAddonServerUp: Bool {
        guard let url = URL(string: addonUrl) else { return false }
        return (try? Data(contentsOf: url)) != nil
    }

    private func launch(route: String) {
        app.terminate()
        app.launchArguments = ["-MoviesDebugAddonUrl", Self.addonUrl, "-MoviesDebugRoute", route]
        app.launch()
    }

    private func primaryButton(beginningWith prefix: String) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", prefix)).firstMatch
    }

    private func streamButton(_ name: String) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label CONTAINS %@", name)).firstMatch
    }

    private func staticText(beginningWith prefix: String) -> XCUIElement {
        app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", prefix)).firstMatch
    }

    private func dismissPlayer(returningTo stream: XCUIElement) {
        for _ in 0..<3 where !stream.hasFocus {
            remote.press(.menu)
            sleep(3)
        }
        XCTAssertTrue(stream.hasFocus)
    }

    private func pressMenu(until element: XCUIElement) {
        for _ in 0..<3 where !element.exists {
            remote.press(.menu)
            _ = element.waitForExistence(timeout: 5)
        }
        XCTAssertTrue(element.exists)
    }

    private func focus(_ element: XCUIElement) {
        let moves: [XCUIRemote.Button] = [.up, .up, .up, .down, .down, .down, .down, .right, .right]
        for direction in moves where !element.hasFocus {
            remote.press(direction)
        }
        XCTAssertTrue(element.hasFocus)
    }

    private func saveScreenshot(_ name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
