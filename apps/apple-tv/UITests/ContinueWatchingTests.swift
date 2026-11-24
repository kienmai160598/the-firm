import XCTest

final class ContinueWatchingTests: XCTestCase {
    private static let series = "details/series/tvostest-series"
    private static let seriesTitle = "Movies tvOS Test Series"
    private static let episodeStream = "Test Episode MP4"
    private static let nextUp = "Next Up"
    private static let nearTheEnd = ["-MoviesDebugStartSecondsBeforeEnd", "4"]

    private var tv: TvApp!

    override func setUpWithError() throws {
        continueAfterFailure = false
        try XCTSkipUnless(TvApp.isAddonServerUp, "Start serve_test_addon.py on port 8765 first")
        tv = TvApp(self)
    }

    func testWatchingPartOfAnEpisodeOffersResumeOnDetails() {
        tv.launch(route: Self.series)

        tv.playEpisode(from: "Play S1 E1", seconds: 6)

        XCTAssertTrue(tv.button(beginningWith: "Resume S1 E1").waitForExistence(timeout: 10))
        tv.screenshot("details-resume")
        XCTAssertTrue(tv.episodeValue(1).hasSuffix("% watched"), "E1: \(tv.episodeValue(1))")
    }

    func testFinishingAnEpisodeOffersTheNextOneOnDetailsAndHome() {
        tv.launch(route: Self.series, extra: Self.nearTheEnd)
        tv.playEpisode(from: "Play S1 E1", seconds: 10)
        XCTAssertTrue(tv.button(beginningWith: Self.nextUp).waitForExistence(timeout: 10))
        XCTAssertEqual(tv.episodeValue(1), "Watched")
        tv.launch(keepingState: true)
        XCTAssertTrue(tv.waitForValue(seriesCard, Self.nextUp, timeout: 60))
        tv.focusCard(seriesCard)

        tv.remote.press(.select)

        XCTAssertTrue(tv.button(containing: Self.episodeStream).waitForExistence(timeout: 30))
        XCTAssertTrue(tv.text(beginningWith: "S1 E2").exists)
        XCTAssertFalse(tv.text(beginningWith: "Resume from").exists)
        tv.remote.press(.menu)
        XCTAssertTrue(tv.waitForFocus(seriesCard, timeout: 10), "Focused: \(tv.focusedLabel)")
    }

    func testInProgressCardResumesWithoutAskingWhereToStart() {
        tv.launch(route: Self.series)
        tv.playEpisode(from: "Play S1 E1", seconds: 6)
        tv.launch(keepingState: true)
        XCTAssertTrue(seriesCard.waitForExistence(timeout: 60))
        XCTAssertTrue((seriesCard.value as? String ?? "").hasSuffix("left"))
        tv.focusCard(seriesCard)
        tv.screenshot("home-cw-card")
        tv.remote.press(.select)
        let stream = tv.button(containing: Self.episodeStream)
        XCTAssertTrue(stream.waitForExistence(timeout: 30))
        XCTAssertTrue(tv.text(beginningWith: "Resume from").waitForExistence(timeout: 10))
        tv.screenshot("cw-streams-resume")

        tv.select(stream)

        XCTAssertFalse(tv.app.buttons["Start from beginning"].waitForExistence(timeout: 4))
    }

    func testRemovingAnInProgressSeriesFallsBackToNextUp() {
        tv.launch(route: Self.series, extra: Self.nearTheEnd)
        tv.playEpisode(from: "Play S1 E1", seconds: 10)
        tv.launch(route: Self.series, keepingState: true)
        tv.playEpisode(from: Self.nextUp, seconds: 6)
        tv.launch(keepingState: true)
        XCTAssertTrue(seriesCard.waitForExistence(timeout: 60))
        XCTAssertTrue((seriesCard.value as? String ?? "").hasSuffix("left"))

        tv.removeFromContinueWatching(seriesCard)

        XCTAssertTrue(tv.waitForValue(seriesCard, Self.nextUp, timeout: 60))
        tv.launch(route: Self.series, keepingState: true)
        XCTAssertTrue(tv.button(beginningWith: Self.nextUp).waitForExistence(timeout: 60))
        XCTAssertFalse(tv.episodeValue(2).hasSuffix("% watched"), "E2: \(tv.episodeValue(2))")
    }

    func testRemovedCardsStayGoneAfterRelaunch() {
        tv.launch(route: Self.series, extra: Self.nearTheEnd)
        tv.playEpisode(from: "Play S1 E1", seconds: 10)
        let movie = tv.watchTestMovie()
        tv.launch(keepingState: true)
        XCTAssertTrue(tv.waitForValue(seriesCard, Self.nextUp, timeout: 60))

        tv.removeFromContinueWatching(seriesCard)
        XCTAssertTrue(tv.waitForDisappearance(seriesCard))
        XCTAssertTrue(tv.waitForFocus(movie), "Focus lost after removing a card")
        tv.removeFromContinueWatching(movie)
        XCTAssertTrue(tv.waitForDisappearance(movie))
        tv.launch(keepingState: true)

        XCTAssertTrue(tv.heroButton.waitForExistence(timeout: 60))
        XCTAssertFalse(seriesCard.waitForExistence(timeout: 10), "Next Up came back")
        XCTAssertFalse(movie.exists, "Removed movie came back")
    }

    func testDownFromHeroFocusesFirstCardAndBackRestoresTheOpenedCard() {
        tv.launch(route: Self.series)
        tv.playEpisode(from: "Play S1 E1", seconds: 6)
        _ = tv.watchTestMovie()
        tv.launch(keepingState: true)
        XCTAssertTrue(seriesCard.waitForExistence(timeout: 60))
        let cards = tv.continueWatchingCards
        XCTAssertEqual(cards.count, 2)
        tv.focusHero()
        tv.remote.press(.down)
        XCTAssertTrue(tv.waitForFocus(cards[0]), "Focused: \(tv.focusedLabel)")
        tv.remote.press(.right)
        XCTAssertTrue(tv.waitForFocus(cards[1]), "Focused: \(tv.focusedLabel)")

        tv.remote.press(.select)

        XCTAssertTrue(tv.app.staticTexts["Streams"].waitForExistence(timeout: 30))
        tv.remote.press(.menu)
        XCTAssertTrue(tv.waitForFocus(cards[1], timeout: 10), "Focused: \(tv.focusedLabel)")
    }

    private var seriesCard: XCUIElement {
        tv.continueWatchingCard(titled: Self.seriesTitle)
    }
}
