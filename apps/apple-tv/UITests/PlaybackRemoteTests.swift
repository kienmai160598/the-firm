import XCTest

final class PlaybackRemoteTests: XCTestCase {
    private static let addonUrl = "http://127.0.0.1:8765/manifest.json"
    private static let bigBuckBunny = "details/movie/tt1254207"
    private static let streamName = "Test 720p MP4"

    private let app = XCUIApplication()
    private let remote = XCUIRemote.shared

    override func setUpWithError() throws {
        continueAfterFailure = false
        try XCTSkipUnless(Self.isAddonServerUp, "Start the TestAddon server on port 8765 first")
        app.launchArguments = [
            "-MoviesDebugAddonUrl", Self.addonUrl,
            "-MoviesDebugRoute", Self.bigBuckBunny,
        ]
    }

    func testHttpStreamPlaysAndSavesProgress() {
        app.launch()
        let primary = app.buttons
            .matching(NSPredicate(format: "label BEGINSWITH 'Play' OR label BEGINSWITH 'Resume'"))
            .firstMatch
        XCTAssertTrue(primary.waitForExistence(timeout: 60))
        saveScreenshot("p1-details-before")

        focus(primary)
        remote.press(.select)
        let stream = streamButton
        XCTAssertTrue(stream.waitForExistence(timeout: 30))
        saveScreenshot("p1b-streams")
        focus(stream)
        remote.press(.select)
        startOverWhenOffered()

        sleep(20)
        remote.press(.up)
        sleep(1)
        saveScreenshot("p2-playing-transport")
        sleep(25)

        dismissPlayer(returningTo: stream)
        let resume = button(labelBeginningWith: "Resume")
        pressMenu(until: resume)
        XCTAssertTrue(resumeLabel.waitForExistence(timeout: 5))
        saveScreenshot("p3-details-resume")
        sleep(3)
    }

    func testRelaunchOffersResumeAndStartsThere() {
        app.launch()
        let resume = button(labelBeginningWith: "Resume")
        XCTAssertTrue(resume.waitForExistence(timeout: 60))
        XCTAssertTrue(resumeLabel.waitForExistence(timeout: 5))
        let savedLabel = resumeLabel.label
        saveScreenshot("p4-relaunch-details-resume")

        focus(resume)
        remote.press(.select)
        let stream = streamButton
        XCTAssertTrue(stream.waitForExistence(timeout: 30))
        focus(stream)
        remote.press(.select)

        let resumeChoice = app.buttons[savedLabel].firstMatch
        XCTAssertTrue(resumeChoice.waitForExistence(timeout: 15))
        XCTAssertTrue(app.buttons["Start from beginning"].firstMatch.exists)
        focus(resumeChoice)
        saveScreenshot("p5-resume-choice")
        remote.press(.select)

        sleep(20)
        remote.press(.up)
        sleep(1)
        saveScreenshot("p6-resumed-transport")
        sleep(4)
        remote.press(.playPause)
        sleep(2)
        dismissPlayer(returningTo: stream)
        sleep(3)
    }

    func testTorrentStreamOpensInfoSheet() {
        app.launchArguments = [
            "-MoviesDebugAddonUrl", Self.addonUrl,
            "-MoviesDebugRoute", "streams/movie/tt1727587",
        ]
        app.launch()
        let torrent = app.buttons
            .matching(NSPredicate(format: "label CONTAINS 'Test Torrent'"))
            .firstMatch
        XCTAssertTrue(torrent.waitForExistence(timeout: 60))
        focus(torrent)
        remote.press(.select)
        let note = app.staticTexts
            .matching(NSPredicate(format: "label BEGINSWITH 'This is a torrent'"))
            .firstMatch
        XCTAssertTrue(note.waitForExistence(timeout: 10))
        saveScreenshot("p9-torrent-sheet")
    }

    private static var isAddonServerUp: Bool {
        guard let url = URL(string: addonUrl) else { return false }
        return (try? Data(contentsOf: url)) != nil
    }

    private func startOverWhenOffered() {
        let startOver = app.buttons["Start from beginning"].firstMatch
        guard startOver.waitForExistence(timeout: 5) else { return }
        focus(startOver)
        remote.press(.select)
    }

    private var streamButton: XCUIElement {
        app.buttons
            .matching(NSPredicate(format: "label CONTAINS %@", Self.streamName))
            .firstMatch
    }

    private var resumeLabel: XCUIElement {
        app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH 'Resume from'")).firstMatch
    }

    private func button(labelBeginningWith prefix: String) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", prefix)).firstMatch
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
        for direction in [XCUIRemote.Button.up, .up, .up, .down, .down, .down, .down]
        where !element.hasFocus {
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
