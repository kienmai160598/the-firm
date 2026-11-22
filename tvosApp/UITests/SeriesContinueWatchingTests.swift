import XCTest

final class SeriesContinueWatchingTests: XCTestCase {
    private static let addonUrl = "http://127.0.0.1:8765/manifest.json"
    private static let testSeries = "details/series/tvostest-series"
    private static let tearsOfSteelStreams = "streams/movie/tt2285752"
    private static let episodeStreamName = "Test Episode MP4"
    private static let seriesTitle = "Movies tvOS Test Series"
    private static let nextUpLabel = "Next Up"
    private static let removeTitle = "Remove from Continue Watching"

    private let app = XCUIApplication()
    private let remote = XCUIRemote.shared

    override func setUpWithError() throws {
        continueAfterFailure = false
        try XCTSkipUnless(Self.isAddonServerUp, "Start the TestAddon server on port 8765 first")
    }

    func testA_SeriesResumeNextUpAndContinueWatching() {
        launch(route: Self.testSeries)
        XCTAssertTrue(primaryButton(beginningWith: "Play S1 E1").waitForExistence(timeout: 60))
        saveScreenshot("s4-1-series-details-start")

        playEpisodeFromPrimary(beginningWith: "Play S1 E1", watchSeconds: 25)
        XCTAssertTrue(primaryButton(beginningWith: "Resume S1 E1").waitForExistence(timeout: 10))
        XCTAssertTrue(episodeValue(1).hasSuffix("% watched"), "E1 value: \(episodeValue(1))")
        saveScreenshot("s4-3-series-resume-e1")

        launch(route: Self.testSeries, extra: ["-MoviesDebugStartSecondsBeforeEnd", "8"])
        playEpisodeFromPrimary(beginningWith: "Resume S1 E1", watchSeconds: 25)
        XCTAssertTrue(primaryButton(beginningWith: "Next Up").waitForExistence(timeout: 10))
        XCTAssertEqual(episodeValue(1), "Watched")
        saveScreenshot("s4-4-series-next-up-e2")

        checkNextUpCardOpensNextEpisode()
        launch(route: Self.testSeries)
        XCTAssertTrue(primaryButton(beginningWith: "Next Up").waitForExistence(timeout: 60))
        playEpisodeFromPrimary(beginningWith: "Next Up", watchSeconds: 30)
        XCTAssertTrue(primaryButton(beginningWith: "Resume S1 E2").waitForExistence(timeout: 10))
        XCTAssertEqual(episodeValue(1), "Watched")
        XCTAssertTrue(episodeValue(2).hasSuffix("% watched"), "E2 value: \(episodeValue(2))")
        saveScreenshot("s4-5-series-resume-e2-progress")

        pressMenu(until: continueWatchingCard)
        XCTAssertTrue(continueWatchingCard.waitForExistence(timeout: 10))
        let cardValue = continueWatchingCard.value as? String ?? ""
        XCTAssertTrue(cardValue.hasSuffix("left"), "Card value: \(cardValue)")
        focusContinueWatchingCard()
        saveScreenshot("s4-6-home-continue-watching")

        remote.press(.select)
        let stream = streamButton(Self.episodeStreamName)
        XCTAssertTrue(stream.waitForExistence(timeout: 30))
        XCTAssertTrue(resumeBanner.waitForExistence(timeout: 10))
        saveScreenshot("s4-7-cw-streams-resume-banner")
        focus(stream)
        remote.press(.select)
        XCTAssertFalse(app.buttons["Start from beginning"].waitForExistence(timeout: 5))
        sleep(15)
        remote.press(.up)
        sleep(1)
        saveScreenshot("s4-8-cw-resumed-playing")
        dismissPlayer(returningTo: stream)
        remote.press(.menu)
        XCTAssertTrue(continueWatchingCard.waitForExistence(timeout: 10))
    }

    func testB_UnplayableAndBrokenStreamsShowMessages() {
        launch(route: Self.tearsOfSteelStreams)
        let mkv = streamButton("Test MKV")
        XCTAssertTrue(mkv.waitForExistence(timeout: 60))
        focus(mkv)
        remote.press(.select)
        let notice = app.staticTexts["Apple TV cannot play this stream"]
        XCTAssertTrue(notice.waitForExistence(timeout: 10))
        XCTAssertTrue(staticText(beginningWith: "MKV files are not supported").exists)
        saveScreenshot("s4-9-unplayable-mkv")
        remote.press(.menu)

        let broken = streamButton("Test Broken MP4")
        XCTAssertTrue(broken.waitForExistence(timeout: 10))
        focus(broken)
        remote.press(.select)
        startOverWhenOffered()
        XCTAssertTrue(app.staticTexts["Playback failed"].waitForExistence(timeout: 30))
        XCTAssertTrue(staticText(beginningWith: "The player stopped with an error").exists)
        saveScreenshot("s4-10-player-failure")
        remote.press(.menu)
        XCTAssertTrue(broken.waitForExistence(timeout: 10))
        let play = primaryButton(beginningWith: "Play")
        pressMenu(until: play)
        XCTAssertFalse(primaryButton(beginningWith: "Resume").exists)
    }

    func testC_ColdLaunchHomeShowsContinueWatchingFirst() {
        app.terminate()
        app.launchArguments = ["-MoviesDebugAddonUrl", Self.addonUrl]
        app.launch()
        XCTAssertTrue(continueWatchingCard.waitForExistence(timeout: 60))
        focusContinueWatchingCard()
        saveScreenshot("s4-11-cold-home-continue-watching")
        remote.press(.select)
        let stream = streamButton(Self.episodeStreamName)
        XCTAssertTrue(stream.waitForExistence(timeout: 30))
        XCTAssertTrue(resumeBanner.waitForExistence(timeout: 10))
        sleep(2)
        let title = app.staticTexts[Self.seriesTitle].firstMatch
        XCTAssertGreaterThan(title.frame.minY, 40, "Streams title frame: \(title.frame)")
        saveScreenshot("s4-12-cw-streams-cold")
        remote.press(.menu)
        XCTAssertTrue(continueWatchingCard.waitForExistence(timeout: 10))
    }

    func testD_ExtensionlessStreamsAreProbed() {
        launch(route: Self.tearsOfSteelStreams)
        let garbage = streamButton("Test Garbage no extension")
        XCTAssertTrue(garbage.waitForExistence(timeout: 60))
        focus(garbage)
        remote.press(.select)
        let notice = app.staticTexts["Apple TV cannot play this stream"]
        XCTAssertTrue(notice.waitForExistence(timeout: 15))
        saveScreenshot("s4-13-probe-garbage")
        remote.press(.menu)

        let stalled = streamButton("Test Stalled link")
        XCTAssertTrue(stalled.waitForExistence(timeout: 10))
        focus(stalled)
        let selectedAt = Date()
        remote.press(.select)
        let checkingText = app.staticTexts["Checking stream"]
        XCTAssertTrue(checkingText.waitForExistence(timeout: 3))
        XCTAssertLessThan(checkingText.frame.maxX, stalled.frame.minX)
        saveScreenshot("s5-10-probe-checking-in-header")
        let checkingGone = NSPredicate(format: "exists == false")
        let checking = app.staticTexts["Checking stream"]
        let finished = expectation(for: checkingGone, evaluatedWith: checking)
        wait(for: [finished], timeout: 15)
        print("Stalled probe finished after \(Date().timeIntervalSince(selectedAt)) s")
        sleep(2)
        saveScreenshot("s4-15-probe-timeout-player")
        dismissPlayer(returningTo: stalled)
        focus(garbage)
        remote.press(.select)
        XCTAssertTrue(notice.waitForExistence(timeout: 15))
        remote.press(.menu)
    }

    func testE_DownFromTabBarFocusesFirstCardAndBackRestoresSelection() {
        app.terminate()
        app.launchArguments = ["-MoviesDebugAddonUrl", Self.addonUrl]
        app.launch()
        let anyCard = app.buttons["continueWatchingCard"].firstMatch
        XCTAssertTrue(anyCard.waitForExistence(timeout: 60))
        let cards = continueWatchingCards
        XCTAssertGreaterThanOrEqual(cards.count, 2)

        let homeTab = app.buttons["Home"].firstMatch
        for _ in 0..<3 where !homeTab.hasFocus {
            remote.press(.up)
        }
        XCTAssertTrue(homeTab.hasFocus)
        remote.press(.down)
        XCTAssertTrue(waitForFocus(cards[0]), "Focused: \(focusedLabel)")
        saveScreenshot("s5-8-down-focuses-first-card")

        remote.press(.right)
        XCTAssertTrue(waitForFocus(cards[1]), "Focused: \(focusedLabel)")
        remote.press(.select)
        XCTAssertTrue(app.staticTexts["Streams"].waitForExistence(timeout: 30))
        remote.press(.menu)
        XCTAssertTrue(cards[1].waitForExistence(timeout: 10))
        XCTAssertTrue(waitForFocus(cards[1]), "Focused: \(focusedLabel)")
        saveScreenshot("s5-9-back-restores-selected-card")
    }

    func testF_RemovingInProgressSeriesFallsBackToNextUp() {
        launchHome()
        XCTAssertTrue(continueWatchingCard.waitForExistence(timeout: 60))
        let cardValue = continueWatchingCard.value as? String ?? ""
        XCTAssertTrue(cardValue.hasSuffix("left"), "Card value: \(cardValue)")
        remove(continueWatchingCard, screenshot: "s6-3-remove-menu-in-progress-series")
        XCTAssertTrue(waitForValue(continueWatchingCard, Self.nextUpLabel, timeout: 60))
        focusCard(continueWatchingCard)
        saveScreenshot("s6-4-in-progress-series-removed-shows-next-up")

        launch(route: Self.testSeries)
        XCTAssertTrue(primaryButton(beginningWith: "Next Up").waitForExistence(timeout: 60))
        XCTAssertFalse(primaryButton(beginningWith: "Resume").exists)
        XCTAssertEqual(episodeValue(1), "Watched")
        XCTAssertFalse(episodeValue(2).hasSuffix("% watched"), "E2 value: \(episodeValue(2))")
        saveScreenshot("s6-5-series-details-after-remove")
    }

    func testG_RemoveFromContinueWatchingDismissesNextUpAndClearsProgress() {
        finishSecondEpisode()

        launchHome()
        XCTAssertTrue(continueWatchingCard.waitForExistence(timeout: 60))
        XCTAssertTrue(waitForValue(continueWatchingCard, Self.nextUpLabel, timeout: 60))
        remove(continueWatchingCard, screenshot: "s5-13-remove-menu-next-up")
        XCTAssertTrue(waitForDisappearance(continueWatchingCard))
        saveScreenshot("s5-14-next-up-removed")

        launchHome()
        let anyCard = app.buttons["continueWatchingCard"].firstMatch
        XCTAssertTrue(anyCard.waitForExistence(timeout: 60))
        sleep(10)
        XCTAssertFalse(continueWatchingCard.exists, "Dismissed Next Up came back after relaunch")

        let otherTitle = continueWatchingCards.map(\.label).first { $0 != Self.seriesTitle }
        guard let otherTitle else { return XCTFail("No in-progress card left to remove") }
        let otherCard = continueWatchingCard(titled: otherTitle)
        remove(otherCard, screenshot: "s5-15-remove-menu-in-progress")
        XCTAssertTrue(waitForDisappearance(otherCard))
        saveScreenshot("s5-16-in-progress-removed")

        launchHome()
        XCTAssertTrue(app.tabBars.firstMatch.waitForExistence(timeout: 30))
        sleep(10)
        XCTAssertFalse(otherCard.exists, "Removed \(otherTitle) came back after relaunch")
    }

    private func finishSecondEpisode() {
        launch(route: Self.testSeries, extra: ["-MoviesDebugStartSecondsBeforeEnd", "8"])
        let nextUp = primaryButton(beginningWith: "Next Up")
        let resume = primaryButton(beginningWith: "Resume S1 E2")
        let eitherShown = NSPredicate { _, _ in nextUp.exists || resume.exists }
        let shown = expectation(for: eitherShown, evaluatedWith: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [shown], timeout: 60), .completed)
        if resume.exists {
            playEpisodeFromPrimary(beginningWith: "Resume S1 E2", watchSeconds: 25)
        }
        XCTAssertTrue(nextUp.waitForExistence(timeout: 10))
    }

    private func checkNextUpCardOpensNextEpisode() {
        launchHome()
        XCTAssertTrue(continueWatchingCard.waitForExistence(timeout: 60))
        XCTAssertTrue(waitForValue(continueWatchingCard, Self.nextUpLabel, timeout: 60))
        focusContinueWatchingCard()
        saveScreenshot("s5-11-next-up-card-e2")
        remote.press(.select)
        XCTAssertTrue(streamButton(Self.episodeStreamName).waitForExistence(timeout: 30))
        XCTAssertTrue(staticText(beginningWith: "S1 E2").exists)
        XCTAssertFalse(resumeBanner.exists)
        saveScreenshot("s5-12-next-up-streams-e2")
        remote.press(.menu)
        XCTAssertTrue(continueWatchingCard.waitForExistence(timeout: 10))
        XCTAssertEqual(continueWatchingCard.value as? String, Self.nextUpLabel)
        XCTAssertTrue(waitForFocus(continueWatchingCard), "Focused: \(focusedLabel)")
        saveScreenshot("s5-17-back-to-next-up-card")
    }

    private func remove(_ card: XCUIElement, screenshot: String) {
        focusCard(card)
        remote.press(.select, forDuration: 1.5)
        let removeAction = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label == %@", Self.removeTitle))
            .firstMatch
        XCTAssertTrue(removeAction.waitForExistence(timeout: 10), app.debugDescription)
        saveScreenshot(screenshot)
        remote.press(.select)
    }

    private func focusCard(_ card: XCUIElement) {
        focusContinueWatchingRow()
        for _ in 0..<6 where !card.hasFocus {
            remote.press(.left)
        }
        for _ in 0..<12 where !card.hasFocus {
            remote.press(.right)
        }
        XCTAssertTrue(waitForFocus(card), "Focused: \(focusedLabel)")
    }

    private func focusContinueWatchingRow() {
        let homeTab = app.buttons["Home"].firstMatch
        for _ in 0..<4 where !homeTab.hasFocus {
            remote.press(.up)
        }
        sleep(1)
        remote.press(.down)
        XCTAssertTrue(
            focusedContinueWatchingCard.waitForExistence(timeout: 3), "Focused: \(focusedLabel)"
        )
    }

    private var focusedContinueWatchingCard: XCUIElement {
        let focusedCard = NSPredicate(
            format: "identifier == %@ AND hasFocus == true", "continueWatchingCard"
        )
        return app.buttons.matching(focusedCard).firstMatch
    }

    private func launchHome() {
        app.terminate()
        app.launchArguments = ["-MoviesDebugAddonUrl", Self.addonUrl]
        app.launch()
    }

    private func waitForValue(
        _ element: XCUIElement,
        _ value: String,
        timeout: TimeInterval
    ) -> Bool {
        let matches = NSPredicate(format: "value == %@", value)
        let valueSet = expectation(for: matches, evaluatedWith: element)
        return XCTWaiter.wait(for: [valueSet], timeout: timeout) == .completed
    }

    private func waitForDisappearance(_ element: XCUIElement) -> Bool {
        let gone = expectation(for: NSPredicate(format: "exists == false"), evaluatedWith: element)
        return XCTWaiter.wait(for: [gone], timeout: 10) == .completed
    }

    private var continueWatchingCards: [XCUIElement] {
        app.buttons.matching(identifier: "continueWatchingCard").allElementsBoundByIndex
            .sorted { $0.frame.minX < $1.frame.minX }
            .map { continueWatchingCard(titled: $0.label) }
    }

    private func continueWatchingCard(titled title: String) -> XCUIElement {
        let titled = NSPredicate(
            format: "identifier == %@ AND label == %@", "continueWatchingCard", title
        )
        return app.buttons.matching(titled).firstMatch
    }

    private func waitForFocus(_ element: XCUIElement) -> Bool {
        let hasFocus = NSPredicate(format: "hasFocus == true")
        let focused = expectation(for: hasFocus, evaluatedWith: element)
        return XCTWaiter.wait(for: [focused], timeout: 3) == .completed
    }

    private var focusedLabel: String {
        app.buttons.matching(NSPredicate(format: "hasFocus == true")).firstMatch.label
    }

    private static var isAddonServerUp: Bool {
        guard let url = URL(string: addonUrl) else { return false }
        return (try? Data(contentsOf: url)) != nil
    }

    private func launch(route: String, extra: [String] = []) {
        app.terminate()
        app.launchArguments = ["-MoviesDebugAddonUrl", Self.addonUrl, "-MoviesDebugRoute", route]
            + extra
        app.launch()
    }

    private func playEpisodeFromPrimary(beginningWith prefix: String, watchSeconds: UInt32) {
        let primary = primaryButton(beginningWith: prefix)
        XCTAssertTrue(primary.waitForExistence(timeout: 60))
        focus(primary)
        remote.press(.select)
        let stream = streamButton(Self.episodeStreamName)
        XCTAssertTrue(stream.waitForExistence(timeout: 30))
        focus(stream)
        remote.press(.select)
        startOverWhenOffered()
        sleep(watchSeconds)
        if prefix.hasPrefix("Next Up") {
            remote.press(.up)
            sleep(1)
            saveScreenshot("s4-2-episode-playing")
        }
        dismissPlayer(returningTo: stream)
        remote.press(.menu)
    }

    private var continueWatchingCard: XCUIElement {
        continueWatchingCard(titled: Self.seriesTitle)
    }

    private var resumeBanner: XCUIElement {
        staticText(beginningWith: "Resume from")
    }

    private func primaryButton(beginningWith prefix: String) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", prefix)).firstMatch
    }

    private func episode(_ number: Int) -> XCUIElement {
        app.buttons["episode-tvostest-series:1:\(number)"].firstMatch
    }

    private func episodeValue(_ number: Int) -> String {
        episode(number).value as? String ?? ""
    }

    private func streamButton(_ name: String) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label CONTAINS %@", name)).firstMatch
    }

    private func staticText(beginningWith prefix: String) -> XCUIElement {
        app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", prefix)).firstMatch
    }

    private func startOverWhenOffered() {
        let startOver = app.buttons["Start from beginning"].firstMatch
        guard startOver.waitForExistence(timeout: 5) else { return }
        focus(startOver)
        remote.press(.select)
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
        focus(element, moving: moves)
    }

    private func focusContinueWatchingCard() {
        focus(continueWatchingCard, moving: [.up, .up, .up, .down, .left, .left, .left])
    }

    private func focus(_ element: XCUIElement, moving moves: [XCUIRemote.Button]) {
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
