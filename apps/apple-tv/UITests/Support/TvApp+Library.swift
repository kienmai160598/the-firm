import XCTest

extension TvApp {
    static let libraryButtonId = "details-library"
    static let libraryStatusId = "library-status"

    var libraryButton: XCUIElement {
        app.buttons.matching(identifier: Self.libraryButtonId).firstMatch
    }

    var libraryStatus: XCUIElement {
        app.descendants(matching: .any).matching(identifier: Self.libraryStatusId).firstMatch
    }

    /// Presses the Library button on Details (right of Play) and waits for its new label.
    func toggleLibrary(expecting label: String, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertTrue(libraryButton.waitForExistence(timeout: 60), file: file, line: line)
        focus(libraryButton, moving: [.right, .right], file: file, line: line)
        remote.press(.select)
        XCTAssertTrue(
            waitUntil(libraryButton, NSPredicate(format: "label == %@", label), timeout: 10),
            "Library button says \(libraryButton.label)",
            file: file,
            line: line
        )
    }

    /// Opens the Library tab from Home through the sidebar.
    func openLibraryTab(file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertTrue(heroButton.waitForExistence(timeout: 60), file: file, line: line)
        focusHero()
        remote.press(.menu)
        for _ in 0..<4 where focusedTabTitle != "Library" {
            remote.press(.down)
            _ = waitForTabTitle("Library")
        }
        XCTAssertEqual(focusedTabTitle, "Library", file: file, line: line)
        remote.press(.select)
    }

    private func waitForTabTitle(_ title: String) -> Bool {
        let tab = NSPredicate { _, _ in self.focusedTabTitle == title }
        let expectation = XCTNSPredicateExpectation(predicate: tab, object: nil)
        return XCTWaiter.wait(for: [expectation], timeout: 1) == .completed
    }
}
