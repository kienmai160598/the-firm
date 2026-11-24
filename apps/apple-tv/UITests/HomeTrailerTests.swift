import XCTest

/// New rows and the hero trailer on Home with the default Cinemeta addon. Keeps the app state, so
/// it is safe on a real TV. Needs the network: the trailer comes from YouTube.
final class HomeTrailerTests: XCTestCase {
    private static let popularMovies = "Popular - Movies"
    private static let newMovies = "New - Movies"
    private static let homeTimeout: TimeInterval = 60
    /// The 3 s wait, the trailer lookup and the first video frame.
    private static let trailerTimeout: TimeInterval = 40
    private static let fadeTimeout: TimeInterval = 5
    /// Long enough for the letterbox crop to settle before the screenshot.
    private static let letterboxSettle: TimeInterval = 4

    private lazy var tv = TvApp(self)

    override func setUp() {
        continueAfterFailure = false
    }

    func testH7NewMoviesFollowsPopularMovies() {
        tv.launch(keepingState: true, withTestAddon: false)
        let popular = tv.app.staticTexts[Self.popularMovies]
        let new = tv.app.staticTexts[Self.newMovies]
        XCTAssertTrue(popular.waitForExistence(timeout: Self.homeTimeout))
        XCTAssertTrue(new.waitForExistence(timeout: Self.homeTimeout))

        XCTAssertGreaterThan(new.frame.minY, popular.frame.minY)
        tv.remote.press(.down)
        tv.remote.press(.down)
        tv.screenshot("home-h7-new-movies")
    }

    func testH8FocusedHeroPlaysItsTrailerAndStopsWhenFocusLeaves() {
        tv.launch(keepingState: true, withTestAddon: false)
        XCTAssertTrue(tv.heroButton.waitForExistence(timeout: Self.homeTimeout))
        XCTAssertTrue(tv.waitForFocus(tv.heroButton, timeout: 5), "Focused: \(tv.focusedLabel)")
        let trailer = tv.app.descendants(matching: .any)["homeHeroTrailer"]
        let focusedAt = Date()

        XCTAssertTrue(tv.waitForValue(trailer, "playing", timeout: Self.trailerTimeout))
        print("Hero trailer showed \(Date().timeIntervalSince(focusedAt)) s after focus")
        let screen = tv.app.frame
        // A letterboxed trailer is zoomed past the screen edges, so it covers rather than matches.
        XCTAssertLessThanOrEqual(trailer.frame.minY, 0, "The trailer leaves a band above it")
        XCTAssertGreaterThanOrEqual(trailer.frame.maxY, screen.maxY, "The trailer stops above the bottom")
        XCTAssertLessThanOrEqual(trailer.frame.minX, 0, "The trailer leaves a band on the left")
        XCTAssertGreaterThanOrEqual(trailer.frame.maxX, screen.maxX, "The trailer leaves a band on the right")
        tv.watch(seconds: Self.letterboxSettle)
        tv.screenshot("home-h8-trailer-playing")

        tv.remote.press(.down)
        XCTAssertTrue(tv.waitForDisappearance(trailer, timeout: Self.fadeTimeout))
        tv.screenshot("home-h8-trailer-stopped")
    }
}
