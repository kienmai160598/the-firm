import XCTest

/// W1 to W3 with the local watched list: a fresh launch has no Trakt sign in.
final class WatchedTests: XCTestCase {
    private static let episodeRow = "episode-"
    private static let seasonRow = "season-"

    private var tv: TvApp!

    override func setUpWithError() throws {
        continueAfterFailure = false
        try XCTSkipUnless(TvApp.isAddonServerUp, "Start serve_test_addon.py on port 8765 first")
        tv = TvApp(self)
    }

    func testW1_markingAContinueWatchingEpisodeWatchedFocusesTheSeriesNextUp() {
        tv.launch(route: TvApp.testSeries)
        tv.playEpisode(from: "Play S1 E1", seconds: 6)
        tv.launch(keepingState: true)
        XCTAssertTrue(seriesCard.waitForExistence(timeout: 60))
        XCTAssertTrue((seriesCard.value as? String ?? "").hasSuffix("left"))
        tv.focusCard(seriesCard)

        tv.chooseFromContextMenu("Mark as Watched")

        XCTAssertTrue(tv.waitForValue(seriesCard, TvApp.nextUpPrefix, timeout: 60))
        XCTAssertTrue(tv.waitForFocus(seriesCard, timeout: 10), "Focused: \(tv.focusedLabel)")
        tv.screenshot("cw-marked-watched")
        tv.launch(route: TvApp.testSeries, keepingState: true)
        XCTAssertTrue(tv.button(beginningWith: TvApp.nextUpPrefix).waitForExistence(timeout: 60))
        XCTAssertEqual(tv.episodeValue(1), "Watched")
    }

    func testW1_withANeighbourFocusMovesToItAndStaysThere() {
        tv.launch(route: TvApp.testSeries)
        tv.playEpisode(from: "Play S1 E1", seconds: 6)
        let movie = tv.watchTestMovie()
        tv.launch(keepingState: true)
        XCTAssertTrue(seriesCard.waitForExistence(timeout: 60))
        tv.focusCard(seriesCard)

        tv.chooseFromContextMenu("Mark as Watched")

        XCTAssertTrue(tv.waitForValue(seriesCard, TvApp.nextUpPrefix, timeout: 60))
        XCTAssertTrue(tv.waitForFocus(movie, timeout: 5), "Focused: \(tv.focusedLabel)")
        _ = XCTWaiter.wait(for: [XCTestExpectation(description: "Next Up settles")], timeout: 3)
        XCTAssertTrue(movie.hasFocus, "Focus left the neighbour: \(tv.focusedLabel)")
    }

    func testW2_anEpisodeMarkedWatchedStaysWatchedAfterARelaunchAndCanBeUnmarked() {
        tv.launch(route: TvApp.testSeries)
        tv.focusOnDetails(tv.episodeCard(2), inRow: Self.episodeRow)

        tv.chooseFromContextMenu("Mark as Watched")

        XCTAssertTrue(tv.waitForValue(tv.episodeCard(2), "Watched", timeout: 10))
        tv.screenshot("episode-marked-watched")
        tv.launch(route: TvApp.testSeries, keepingState: true)
        XCTAssertTrue(tv.waitForValue(tv.episodeCard(2), "Watched", timeout: 60))
        tv.focusOnDetails(tv.episodeCard(2), inRow: Self.episodeRow)
        tv.chooseFromContextMenu("Mark as Unwatched")
        XCTAssertTrue(tv.waitUntil(tv.episodeCard(2), NSPredicate(format: "value != %@", "Watched"), timeout: 10))
        XCTAssertNotEqual(tv.episodeValue(1), "Watched")
    }

    func testW3_theSeasonMenuMarksEveryReleasedEpisodeAndThenUnmarksThem() {
        tv.launch(route: TvApp.testSeries)
        tv.focusOnDetails(tv.seasonTab, inRow: Self.seasonRow)

        tv.chooseFromContextMenu("Mark Season as Watched")

        for number in 1...3 {
            XCTAssertTrue(tv.waitForValue(tv.episodeCard(number), "Watched", timeout: 10), "E\(number)")
        }
        tv.screenshot("season-marked-watched")
        XCTAssertTrue(tv.waitForFocus(tv.seasonTab), "Focused: \(tv.focusedLabel)")
        tv.chooseFromContextMenu("Mark Season as Unwatched")
        for number in 1...3 {
            XCTAssertTrue(
                tv.waitUntil(tv.episodeCard(number), NSPredicate(format: "value != %@", "Watched"), timeout: 10),
                "E\(number)"
            )
        }
    }

    private var seriesCard: XCUIElement {
        tv.continueWatchingCard(titled: TvApp.testSeriesTitle)
    }
}
