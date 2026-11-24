import XCTest

extension TvApp {
    /// Opens the Settings tab from Home and then the page with this title ("Addons", ...). The
    /// focused element is the list cell, so rows are found by their label.
    func openSettings(page title: String, file: StaticString = #filePath, line: UInt = #line) {
        openSettingsTab(file: file, line: line)
        openSettingsPage(title, file: file, line: line)
    }

    /// Opens a page from the Settings list that is on screen.
    func openSettingsPage(_ title: String, file: StaticString = #filePath, line: UInt = #line) {
        let row = NSPredicate(format: "label BEGINSWITH %@", title)
        for _ in 0..<5 where !focusedLabel.hasPrefix(title) {
            remote.press(.down)
            _ = waitUntil(focused, row, timeout: 1)
        }
        XCTAssertTrue(focusedLabel.hasPrefix(title), "Focused \(focusedLabel)", file: file, line: line)
        remote.press(.select)
    }

    func openSettingsTab(file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertTrue(heroButton.waitForExistence(timeout: 60), file: file, line: line)
        focusHero()
        remote.press(.menu)
        for _ in 0..<5 where focusedTabTitle != "Settings" {
            remote.press(.down)
            _ = waitUntilTab("Settings")
        }
        XCTAssertEqual(focusedTabTitle, "Settings", file: file, line: line)
        remote.press(.select)
        let firstRow = app.buttons["settings-account"]
        XCTAssertTrue(firstRow.waitForExistence(timeout: 10), file: file, line: line)
        _ = waitUntil(focused, NSPredicate(format: "label BEGINSWITH 'Account'"), timeout: 3)
    }

    private func waitUntilTab(_ title: String) -> Bool {
        let tab = NSPredicate { _, _ in self.focusedTabTitle == title }
        let expectation = XCTNSPredicateExpectation(predicate: tab, object: nil)
        return XCTWaiter.wait(for: [expectation], timeout: 1) == .completed
    }
}
