import XCTest

/// The trailer behind a title on Details, with the default Cinemeta addon. Keeps the app state, so
/// it is safe on a real TV. Needs the network: the trailer comes from YouTube.
final class DetailsTrailerTests: XCTestCase {
    /// Inception, a Cinemeta title with a YouTube trailer.
    private static let details = "details/movie/tt1375666"
    /// The 1 s wait, the trailer lookup and the first video frame.
    private static let trailerTimeout: TimeInterval = 40
    private static let fadeTimeout: TimeInterval = 5
    private static let letterboxSettle: TimeInterval = 4

    private lazy var tv = TvApp(self)

    override func setUp() {
        continueAfterFailure = false
    }

    func testD6FocusedPlayButtonPlaysTheTrailerAndScrollingDownStopsIt() {
        tv.launch(route: Self.details, keepingState: true, withTestAddon: false)
        let trailer = tv.app.descendants(matching: .any)["heroTrailer"]

        XCTAssertTrue(tv.waitForValue(trailer, "playing", timeout: Self.trailerTimeout))
        tv.watch(seconds: Self.letterboxSettle)
        tv.screenshot("details-d6-trailer-playing")

        tv.remote.press(.down)
        XCTAssertTrue(tv.waitForDisappearance(trailer, timeout: Self.fadeTimeout))
        tv.screenshot("details-d6-trailer-stopped")
    }
}
