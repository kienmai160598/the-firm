import XCTest

/// Streams layout on the debug fixture. Keeps the app state, so it is safe on a real TV.
final class StreamsLayoutTests: XCTestCase {
    private static let route = "streams/movie/tt1160419"
    private static let fillerRows = 40
    private static let rowsWalked = 12

    private lazy var tv = TvApp(self)

    func testS7TitleStaysInViewWhileTheListScrolls() {
        tv.launch(route: Self.route, keepingState: true, withTestAddon: false, extra: [
            "-MoviesDebugStreamFixture", "YES",
            "-MoviesDebugStreamFixtureFillerRows", "\(Self.fillerRows)",
        ])
        let title = tv.app.staticTexts["Streams"]
        let firstRow = tv.button(containing: "Fixture Debrid")
        XCTAssertTrue(firstRow.waitForExistence(timeout: 90))
        XCTAssertTrue(tv.waitForFocus(firstRow), "Focused: \(tv.focusedLabel)")
        let titleFrame = title.frame

        for _ in 0..<Self.rowsWalked {
            tv.remote.press(.down)
        }

        let focused = tv.focused
        XCTAssertTrue(focused.label.contains("Fixture Filler"), "Focused: \(focused.label)")
        XCTAssertEqual(title.frame, titleFrame)
        XCTAssertFalse(title.frame.intersects(focused.frame))
        XCTAssertGreaterThan(focused.frame.minX, title.frame.maxX)
        tv.screenshot("streams-s7-scrolled")
    }
}
