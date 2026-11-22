import XCTest

extension TvApp {
    static let continueWatchingId = "continueWatchingCard"
    static let heroId = "homeHeroButton"

    var heroButton: XCUIElement {
        app.buttons.matching(identifier: Self.heroId).firstMatch
    }

    var isHeroFocused: Bool {
        focusedIdentifier == Self.heroId
    }

    var focusedTabTitle: String {
        let current = focused
        guard current.exists else { return "" }
        guard current.label.isEmpty else { return current.label }
        let title = current.staticTexts.firstMatch
        return title.exists ? title.label : ""
    }

    var isTabBarFocused: Bool {
        Self.tabTitles.contains(focusedTabTitle)
    }

    var continueWatchingCards: [XCUIElement] {
        app.buttons.matching(identifier: Self.continueWatchingId).allElementsBoundByIndex
            .sorted { $0.frame.minX < $1.frame.minX }
            .map { continueWatchingCard(titled: $0.label) }
    }

    func continueWatchingCard(titled title: String) -> XCUIElement {
        let titled = NSPredicate(
            format: "identifier == %@ AND label == %@", Self.continueWatchingId, title
        )
        return app.buttons.matching(titled).firstMatch
    }

    func focusHero(file: StaticString = #filePath, line: UInt = #line) {
        for _ in 0..<8 where !isHeroFocused {
            remote.press(isTabBarFocused ? .right : .up)
            _ = waitForHero(focused: true)
        }
        XCTAssertTrue(isHeroFocused, "Focused: \(focusedLabel)", file: file, line: line)
    }

    func focusCard(_ card: XCUIElement, file: StaticString = #filePath, line: UInt = #line) {
        focusHero(file: file, line: line)
        for _ in 0..<3 where isHeroFocused {
            remote.press(.down)
            _ = waitForHero(focused: false)
        }
        for _ in 0..<12 where !card.hasFocus {
            remote.press(.right)
        }
        XCTAssertTrue(waitForFocus(card), "Focused: \(focusedLabel)", file: file, line: line)
    }

    private func waitForHero(focused: Bool) -> Bool {
        let state = NSPredicate { _, _ in self.isHeroFocused == focused }
        let expectation = XCTNSPredicateExpectation(predicate: state, object: nil)
        return XCTWaiter.wait(for: [expectation], timeout: 2) == .completed
    }

    func removeFromContinueWatching(_ card: XCUIElement) {
        focusCard(card)
        remote.press(.select, forDuration: 1.5)
        let remove = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label == %@", "Remove from Continue Watching"))
            .firstMatch
        XCTAssertTrue(remove.waitForExistence(timeout: 10))
        remote.press(.select)
    }
}
