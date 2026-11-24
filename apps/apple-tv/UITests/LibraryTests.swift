import XCTest

/// LB1 to LB3 with the local library: a fresh launch has no Trakt sign in.
final class LibraryTests: XCTestCase {
    private static let series = "details/series/tvostest-series"
    private static let seriesTitle = "Movies tvOS Test Series"

    private var tv: TvApp!

    override func setUpWithError() throws {
        continueAfterFailure = false
        try XCTSkipUnless(TvApp.isAddonServerUp, "Start serve_test_addon.py on port 8765 first")
        tv = TvApp(self)
    }

    func testLB1_LB3_aTitleAddedOnDetailsIsInLibraryAfterARelaunchAndOpensDetails() {
        tv.launch(route: Self.series)
        XCTAssertTrue(tv.libraryButton.waitForExistence(timeout: 60))
        XCTAssertEqual(tv.libraryButton.label, "Add to Library")

        tv.toggleLibrary(expecting: "In Library")
        tv.screenshot("details-in-library")

        tv.launch(keepingState: true)
        tv.openLibraryTab()
        let poster = tv.button(containing: Self.seriesTitle)
        XCTAssertTrue(poster.waitForExistence(timeout: 20))
        tv.screenshot("library-rows")
        tv.focus(poster, moving: [.down, .right, .down, .right])
        tv.remote.press(.select)
        XCTAssertTrue(tv.libraryButton.waitForExistence(timeout: 30))
        XCTAssertEqual(tv.libraryButton.label, "In Library")
    }

    func testLB2_LB3_removingTheTitleLeavesAnEmptyLibraryThatHoldsFocus() {
        tv.launch(route: Self.series)
        tv.toggleLibrary(expecting: "In Library")
        tv.toggleLibrary(expecting: "Add to Library")

        tv.launch(keepingState: true)
        tv.openLibraryTab()

        XCTAssertTrue(tv.libraryStatus.waitForExistence(timeout: 10))
        XCTAssertTrue(tv.app.staticTexts["Your Library is empty"].exists)
        XCTAssertTrue(tv.waitUntil(tv.focused, NSPredicate(format: "exists == true"), timeout: 3))
        XCTAssertFalse(tv.isTabBarFocused, "The empty Library left focus in the sidebar")
        tv.screenshot("library-empty")
    }
}
