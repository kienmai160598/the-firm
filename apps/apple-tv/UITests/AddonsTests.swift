import XCTest

final class AddonsTests: XCTestCase {
    private static let testAddonName = "Nuvio tvOS test films"
    private static let rowId = "addon-row"

    private var tv: TvApp!

    override func setUp() {
        continueAfterFailure = false
        tv = TvApp(self)
    }

    func testChoosingAnAddonOffersItsActionsAndTurnOffMarksItOff() throws {
        try XCTSkipUnless(TvApp.isAddonServerUp, "Test addon server is not running")
        tv.launch()
        openAddonsTab()
        let testAddon = tv.button(containing: Self.testAddonName)
        XCTAssertTrue(testAddon.waitForExistence(timeout: 20))
        XCTAssertTrue(testAddon.label.contains("Streams"), "Row: \(testAddon.label)")
        XCTAssertTrue(waitForRowFocus(), "Focused \(tv.focusedLabel)")
        tv.screenshot("addons-list")

        focusRow(containing: Self.testAddonName)
        tv.remote.press(.select)
        let turnOff = tv.app.buttons["Turn Off"].firstMatch
        XCTAssertTrue(turnOff.waitForExistence(timeout: 5))
        tv.screenshot("addons-actions")
        focusButton(labeled: "Turn Off", moving: .up)
        tv.remote.press(.select)

        let offRow = tv.app.buttons.matching(
            NSPredicate(format: "label CONTAINS %@ AND label ENDSWITH 'Off'", Self.testAddonName)
        ).firstMatch
        XCTAssertTrue(offRow.waitForExistence(timeout: 5), "Row: \(testAddon.label)")
        tv.screenshot("addons-turned-off")
    }

    func testMoveUpPutsTheAddonFirst() throws {
        try XCTSkipUnless(TvApp.isAddonServerUp, "Test addon server is not running")
        tv.launch()
        openAddonsTab()
        XCTAssertTrue(tv.button(containing: Self.testAddonName).waitForExistence(timeout: 20))
        focusRow(containing: Self.testAddonName)
        tv.remote.press(.select)
        XCTAssertTrue(tv.app.buttons["Move Up"].firstMatch.waitForExistence(timeout: 5))

        focusButton(labeled: "Move Up", moving: .down)
        tv.remote.press(.select)

        let firstRow = tv.app.buttons.matching(identifier: Self.rowId).firstMatch
        let startsWithTestAddon = NSPredicate(format: "label BEGINSWITH %@", Self.testAddonName)
        XCTAssertTrue(tv.waitUntil(firstRow, startsWithTestAddon, timeout: 5), firstRow.label)
    }

    func testRemovingEveryAddonShowsTheEmptyState() {
        tv.launch(withTestAddon: false)
        openAddonsTab()
        let emptyState = tv.app.staticTexts["No addons"]

        for _ in 0..<4 where !emptyState.exists {
            removeFirstAddon()
        }

        XCTAssertTrue(emptyState.waitForExistence(timeout: 5))
        tv.screenshot("addons-empty")
    }

    func testAManifestThatCannotBeReachedShowsWhyItFailed() {
        tv.launch(withTestAddon: false)
        openAddonsTab()
        let field = tv.app.textFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        tv.focus(field, moving: [.left, .up, .up])
        tv.remote.press(.select)
        let keyboard = NSPredicate(format: "label == %@", "Keyboard")
        XCTAssertTrue(tv.waitUntil(tv.focused, keyboard, timeout: 5), "Focused \(tv.focusedLabel)")

        field.typeText("http://127.0.0.1:9\n")

        let failure = tv.app.staticTexts["Couldn't add this addon"]
        XCTAssertTrue(failure.waitForExistence(timeout: 30))
        tv.remote.press(.menu)
        XCTAssertTrue(tv.waitForFocus(field), "Focused \(tv.focusedLabel)")
        tv.screenshot("addons-install-failed")
    }

    private func removeFirstAddon() {
        let anyRow = tv.app.buttons[Self.rowId].firstMatch
        XCTAssertTrue(anyRow.waitForExistence(timeout: 20))
        XCTAssertTrue(waitForRowFocus(), "Focused \(tv.focusedLabel)")
        let name = tv.focusedLabel
        tv.remote.press(.select)
        let removeAction = tv.app.buttons["Remove"].firstMatch
        XCTAssertTrue(removeAction.waitForExistence(timeout: 5))
        focusButton(labeled: "Remove", moving: .down)
        tv.remote.press(.select)
        let question = tv.text(beginningWith: "Remove ")
        XCTAssertTrue(question.waitForExistence(timeout: 5), "No confirmation for \(name)")
        tv.screenshot("addons-remove-confirmation")
        focusButton(labeled: "Remove", moving: .right)
        tv.remote.press(.select)
        _ = tv.waitForDisappearance(question, timeout: 5)
    }

    private func focusButton(labeled label: String, moving direction: XCUIRemote.Button) {
        for _ in 0..<5 where tv.focusedLabel != label {
            tv.remote.press(direction)
            _ = tv.waitUntil(tv.focused, NSPredicate(format: "label == %@", label), timeout: 1)
        }
        XCTAssertEqual(tv.focusedLabel, label)
    }

    private func waitForRowFocus() -> Bool {
        let cell = NSPredicate(format: "elementType == %d", XCUIElement.ElementType.cell.rawValue)
        return tv.waitUntil(tv.focused, cell, timeout: 3)
    }

    private func focusRow(containing name: String) {
        for _ in 0..<4 where !tv.focusedLabel.contains(name) {
            tv.remote.press(.down)
        }
        XCTAssertTrue(tv.focusedLabel.contains(name), "Focused \(tv.focusedLabel)")
    }

    private func openAddonsTab() {
        XCTAssertTrue(tv.heroButton.waitForExistence(timeout: 60))
        tv.focusHero()
        tv.remote.press(.menu)
        for _ in 0..<5 where tv.focusedTabTitle != "Addons" {
            tv.remote.press(.down)
            _ = waitForTab("Addons")
        }
        XCTAssertEqual(tv.focusedTabTitle, "Addons")
        tv.remote.press(.select)
        XCTAssertTrue(tv.app.staticTexts["Add an addon"].waitForExistence(timeout: 10))
    }

    private func waitForTab(_ title: String) -> Bool {
        let tab = NSPredicate { _, _ in self.tv.focusedTabTitle == title }
        let expectation = XCTNSPredicateExpectation(predicate: tab, object: nil)
        return XCTWaiter.wait(for: [expectation], timeout: 1) == .completed
    }
}
