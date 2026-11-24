import XCTest

/// Settings: the pages it lists, and a language choice that sticks.
final class SettingsTests: XCTestCase {
    private var tv: TvApp!

    override func setUp() {
        continueAfterFailure = false
        tv = TvApp(self)
    }

    func testST1SettingsListsItsPagesAndAboutShowsTheVersion() {
        tv.launch(withTestAddon: false)
        tv.openSettingsTab()
        for page in ["playback", "subtitles", "addons", "about"] {
            XCTAssertTrue(tv.app.buttons["settings-\(page)"].exists, "No \(page) row")
        }
        tv.screenshot("settings-root")

        tv.openSettingsPage("About")
        let version = tv.app.descendants(matching: .any)
            .matching(NSPredicate(format: "label BEGINSWITH 'Version'")).firstMatch
        XCTAssertTrue(version.waitForExistence(timeout: 5))
        tv.screenshot("settings-about")
    }

    func testST2SubtitlesStartVietnameseAndOffSticks() {
        tv.launch(withTestAddon: false)
        tv.openSettings(page: "Subtitles")
        let selected = NSPredicate(format: "label BEGINSWITH 'Vietnamese'")
        XCTAssertTrue(tv.waitUntil(tv.focused, selected, timeout: 5), "Focused \(tv.focusedLabel)")
        tv.screenshot("settings-subtitles")

        for _ in 0..<90 where !tv.focusedLabel.hasPrefix("Off") {
            tv.remote.press(.up)
        }
        tv.remote.press(.select)
        tv.remote.press(.menu)

        let offRow = tv.app.buttons.matching(
            NSPredicate(format: "identifier == 'settings-subtitles' AND label ENDSWITH 'Off'")
        ).firstMatch
        XCTAssertTrue(offRow.waitForExistence(timeout: 5), "Focused \(tv.focusedLabel)")
    }
}
