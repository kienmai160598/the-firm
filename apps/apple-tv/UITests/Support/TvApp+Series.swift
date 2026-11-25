import XCTest

/// The synthetic test series served by serve_test_addon.py: one season of three released episodes.
extension TvApp {
    static let testSeries = "details/series/tvostest-series"
    static let testSeriesTitle = "Movies tvOS Test Series"
    static let testEpisodeStream = "Test Episode MP4"
    static let nextUpPrefix = "Next Up"

    func episodeCard(_ number: Int) -> XCUIElement {
        app.buttons["episode-tvostest-series:1:\(number)"].firstMatch
    }

    func episodeValue(_ number: Int) -> String {
        episodeCard(number).value as? String ?? ""
    }

    var seasonTab: XCUIElement {
        app.buttons["season-1"].firstMatch
    }

    /// Plays an episode from the primary button on Details and returns to Details once progress is saved.
    func playEpisode(from primaryPrefix: String, seconds: TimeInterval) {
        let primary = button(beginningWith: primaryPrefix)
        let stream = button(containing: Self.testEpisodeStream)
        XCTAssertTrue(primary.waitForExistence(timeout: 60), primaryPrefix)
        select(primary)
        XCTAssertTrue(stream.waitForExistence(timeout: 30))
        select(stream)
        watch(seconds: seconds)
        leavePlayer(from: stream, waitingFor: savedPrimary(replacing: primaryPrefix))
    }

    func leavePlayer(from stream: XCUIElement, waitingFor saved: XCUIElement) {
        XCTAssertTrue(pressMenu(untilFocused: stream))
        remote.press(.menu)
        XCTAssertTrue(saved.waitForExistence(timeout: 15), "Progress not saved")
    }

    /// Presses Menu until the player has closed: AVPlayer takes the first press for its controls.
    func leavePlayer() {
        for _ in 0..<4 where player.exists {
            remote.press(.menu)
            _ = player.waitForNonExistence(timeout: 3)
        }
        XCTAssertFalse(player.exists, "The player is still open")
    }

    func savedPrimary(replacing playedPrefix: String) -> XCUIElement {
        let saved = NSPredicate(
            format: "(label BEGINSWITH %@ OR label BEGINSWITH %@) AND NOT label BEGINSWITH %@",
            "Resume", Self.nextUpPrefix, playedPrefix
        )
        return app.buttons.matching(saved).firstMatch
    }

    /// Plays the Big Buck Bunny test stream for a few seconds and returns its Continue Watching card.
    func watchTestMovie() -> XCUIElement {
        launch(route: "details/movie/tt1254207", keepingState: true)
        let stream = button(containing: "Test Pattern MP4")
        select(button(beginningWith: "Play"))
        XCTAssertTrue(stream.waitForExistence(timeout: 30))
        select(stream)
        watch(seconds: 6)
        leavePlayer(from: stream, waitingFor: savedPrimary(replacing: "Play"))
        return app.buttons.matching(identifier: Self.continueWatchingId)
            .matching(NSPredicate(format: "label != %@", Self.testSeriesTitle))
            .firstMatch
    }

    /// Moves focus down from the Details hero to the row whose identifiers start with [rowPrefix],
    /// then right along it to [element]. The page scrolls, so the row is found by identifier.
    func focusOnDetails(
        _ element: XCUIElement,
        inRow rowPrefix: String,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        XCTAssertTrue(element.waitForExistence(timeout: 60), file: file, line: line)
        let inRow = NSPredicate { _, _ in self.focusedIdentifier.hasPrefix(rowPrefix) }
        for _ in 0..<4 where !focusedIdentifier.hasPrefix(rowPrefix) {
            remote.press(.down)
            _ = XCTWaiter.wait(for: [XCTNSPredicateExpectation(predicate: inRow, object: nil)], timeout: 2)
        }
        for _ in 0..<4 where !element.hasFocus {
            remote.press(.right)
            _ = waitForFocus(element, timeout: 2)
        }
        XCTAssertTrue(element.hasFocus, "Focused: \(focusedLabel)", file: file, line: line)
    }

    /// Long presses the focused element and picks [item] from its context menu. The focused menu
    /// cell has no label; the entry inside it carries the title.
    func chooseFromContextMenu(_ item: String, file: StaticString = #filePath, line: UInt = #line) {
        remote.press(.select, forDuration: 1.5)
        let titled = NSPredicate(format: "label == %@", item)
        let entry = app.descendants(matching: .any).matching(titled).firstMatch
        XCTAssertTrue(entry.waitForExistence(timeout: 10), "No menu item \(item)", file: file, line: line)
        let isOnEntry = { self.focused.descendants(matching: .any).matching(titled).firstMatch.exists }
        let onEntry = NSPredicate { _, _ in isOnEntry() }
        for _ in 0..<4 where !isOnEntry() {
            remote.press(.down)
            _ = XCTWaiter.wait(for: [XCTNSPredicateExpectation(predicate: onEntry, object: nil)], timeout: 2)
        }
        XCTAssertTrue(isOnEntry(), "Menu focus is not on \(item)", file: file, line: line)
        remote.press(.select)
    }
}
