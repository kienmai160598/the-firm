import XCTest

final class NavigationTests: XCTestCase {
    private static let popularMovies = "Popular - Movies"
    private static let breakingBad = "details/series/tt0903747"

    private var tv: TvApp!

    override func setUp() {
        continueAfterFailure = false
        tv = TvApp(self)
    }

    func testHeroArrowsChangeItemThenLeftOpensSidebarAndDownReachesFirstRow() {
        tv.launch(withTestAddon: false)
        XCTAssertTrue(tv.heroButton.waitForExistence(timeout: 60))
        tv.focusHero()
        tv.screenshot("home-hero")
        let heroHeight = tv.heroButton.frame.height
        XCTAssertTrue((66...76).contains(heroHeight), "Hero button height \(heroHeight)")
        let firstTitle = heroTitle
        tv.remote.press(.right)
        XCTAssertTrue(waitForHeroTitle("value != %@", firstTitle), "Right kept the item")
        tv.remote.press(.left)
        XCTAssertTrue(waitForHeroTitle("value == %@", firstTitle), "Left did not go back")

        tv.remote.press(.left)

        XCTAssertTrue(waitForTabBarFocus(), "Left on item 1: \(tv.focusedLabel)")
        tv.remote.press(.right)
        XCTAssertTrue(tv.waitForFocus(tv.heroButton))
        XCTAssertEqual(heroTitle, firstTitle, "Right from sidebar changed the item")
        tv.remote.press(.down)
        XCTAssertTrue(tv.waitForDisappearance(focusedHero, timeout: 3))
        XCTAssertFalse(tv.isTabBarFocused, "Down from hero: \(tv.focusedLabel)")
    }

    func testSidebarReachesEveryTabAndMenuThereLeavesTheApp() {
        tv.launch(withTestAddon: false)
        XCTAssertTrue(tv.heroButton.waitForExistence(timeout: 60))
        tv.focusHero()
        tv.remote.press(.menu)
        XCTAssertTrue(waitForTabBarFocus())
        XCTAssertEqual(tv.focusedTabTitle, "Home", "Menu on Home did not open the sidebar")
        for tab in ["Search", "Library", "Addons"] {
            tv.remote.press(.down)
            XCTAssertTrue(waitForTab(tab), "Expected \(tab), focused \(tv.focusedTabTitle)")
        }
        tv.remote.press(.select)
        XCTAssertTrue(tv.app.staticTexts["Add an addon"].waitForExistence(timeout: 10))
        XCTAssertTrue(tv.waitUntil(tv.focused, NSPredicate(format: "exists == true"), timeout: 3))
        XCTAssertFalse(tv.isTabBarFocused, "Select on Addons kept focus in the sidebar")
        pressMenuUntilTabBar()
        XCTAssertEqual(tv.focusedTabTitle, "Addons")
        tv.remote.press(.down)
        XCTAssertTrue(waitForTab("Settings"))
        tv.remote.press(.select)
        XCTAssertTrue(tv.app.staticTexts["Coming in a later slice"].waitForExistence(timeout: 5))
        pressMenuUntilTabBar()
        XCTAssertEqual(tv.focusedTabTitle, "Settings")

        tv.remote.press(.menu)

        XCTAssertTrue(tv.app.wait(for: .runningBackground, timeout: 10))
    }

    func testTypingATitleInSearchShowsItsPoster() {
        tv.launch(withTestAddon: false)
        let field = openSearchTab()

        field.typeText("Breaking Bad")

        XCTAssertTrue(tv.button(containing: "Breaking Bad").waitForExistence(timeout: 30))
        tv.screenshot("search-results")
    }

    func testOpeningASearchResultKeepsTheQueryInRecentSearches() {
        tv.launch(withTestAddon: false)
        openSearchTab().typeText("Breaking Bad")
        XCTAssertTrue(tv.button(containing: "Breaking Bad").waitForExistence(timeout: 30))
        tv.remote.press(.down)
        XCTAssertTrue(tv.waitUntil(tv.focused, NSPredicate(format: "elementType == %d",
            XCUIElement.ElementType.button.rawValue), timeout: 3))
        tv.remote.press(.select)
        XCTAssertTrue(playButton.waitForExistence(timeout: 30))

        tv.launch(keepingState: true, withTestAddon: false)
        _ = openSearchTab()

        XCTAssertTrue(tv.app.buttons["Breaking Bad"].waitForExistence(timeout: 10))
        tv.screenshot("search-start")
    }

    func testPosterOpensDetailsThenStreamsAndMenuReturnsToTheSameCard() {
        tv.launch(withTestAddon: false)
        XCTAssertTrue(tv.app.staticTexts[Self.popularMovies].waitForExistence(timeout: 60))
        focusFirstPoster()
        tv.remote.press(.right)
        let card = tv.focusedLabel
        tv.remote.press(.select)
        XCTAssertTrue(playButton.waitForExistence(timeout: 30))
        tv.screenshot("details-movie")
        tv.select(playButton)
        XCTAssertTrue(tv.app.staticTexts["Streams"].waitForExistence(timeout: 30))
        XCTAssertTrue(tv.app.staticTexts["Playback unavailable"].waitForExistence(timeout: 30))
        tv.screenshot("navigation-streams-unavailable")

        tv.remote.press(.menu)
        XCTAssertTrue(tv.waitForFocus(playButton, timeout: 10))
        tv.remote.press(.menu)

        XCTAssertTrue(tv.app.staticTexts[Self.popularMovies].waitForExistence(timeout: 10))
        let sameCard = NSPredicate(format: "label == %@", card)
        XCTAssertTrue(tv.waitUntil(tv.focused, sameCard, timeout: 5), "Focused: \(tv.focusedLabel)")
    }

    func testSeasonPickerAndEpisodeCardOpenStreamsAndMenuRestoresTheEpisode() {
        tv.launch(route: Self.breakingBad, withTestAddon: false)
        XCTAssertTrue(playButton.waitForExistence(timeout: 60))
        XCTAssertTrue(tv.app.staticTexts["E1  Pilot"].waitForExistence(timeout: 10))
        tv.remote.press(.down)
        XCTAssertTrue(tv.waitForFocus(tv.app.buttons["Season 1"]))
        tv.remote.press(.right)
        XCTAssertTrue(tv.waitForFocus(tv.app.buttons["Season 2"]))
        tv.remote.press(.select)
        XCTAssertTrue(tv.app.staticTexts["E1  Seven Thirty-Seven"].waitForExistence(timeout: 10))
        XCTAssertFalse(tv.app.staticTexts["E1  Pilot"].exists)
        tv.remote.press(.down)
        tv.remote.press(.right)
        let episode = tv.focusedIdentifier
        XCTAssertTrue(episode.hasPrefix("episode-"), "Focused: \(episode)")

        tv.remote.press(.select)

        XCTAssertTrue(tv.text(beginningWith: "S2 E").waitForExistence(timeout: 30))
        XCTAssertTrue(tv.app.staticTexts["Playback unavailable"].waitForExistence(timeout: 30))
        tv.remote.press(.menu)
        XCTAssertTrue(tv.waitForFocus(tv.app.buttons[episode], timeout: 10))
        XCTAssertTrue(tv.app.staticTexts["E1  Seven Thirty-Seven"].exists, "Season 2 not kept")
    }

    func testCastCaptionMovesDownWhenTheCardIsFocused() {
        tv.launch(route: Self.breakingBad, withTestAddon: false)
        let castTitle = tv.app.staticTexts["Cast & Crew"].firstMatch
        XCTAssertTrue(castTitle.waitForExistence(timeout: 60))
        for _ in 0..<4 where tv.focused.frame.minY <= castTitle.frame.maxY {
            tv.remote.press(.down)
            _ = tv.waitUntil(tv.focused, NSPredicate(format: "exists == true"), timeout: 2)
        }
        tv.remote.press(.right)
        let caption = tv.app.staticTexts[tv.focusedLabel].firstMatch
        let focusedY = caption.frame.minY
        tv.screenshot("navigation-cast-focus")

        tv.remote.press(.left)

        let restingY = caption.frame.minY
        XCTAssertGreaterThan(focusedY, restingY + 4, "Focused caption did not move down")
    }

    private var playButton: XCUIElement {
        tv.button(beginningWith: "Play")
    }

    private var heroTitle: String {
        focusedHero.value as? String ?? ""
    }

    private var focusedHero: XCUIElement {
        let focused = NSPredicate(format: "identifier == %@ AND hasFocus == true", TvApp.heroId)
        return tv.app.buttons.matching(focused).firstMatch
    }

    private func waitForHeroTitle(_ format: String, _ title: String) -> Bool {
        tv.waitUntil(focusedHero, NSPredicate(format: format, title), timeout: 3)
    }

    private func openSearchTab() -> XCUIElement {
        XCTAssertTrue(tv.heroButton.waitForExistence(timeout: 60))
        tv.focusHero()
        tv.remote.press(.menu)
        XCTAssertTrue(waitForTab("Home"))
        tv.remote.press(.down)
        XCTAssertTrue(waitForTab("Search"), "Focused \(tv.focusedTabTitle)")
        tv.remote.press(.select)
        let field = tv.app.searchFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        let keyboard = NSPredicate(format: "label == %@", "Keyboard")
        XCTAssertTrue(tv.waitUntil(tv.focused, keyboard, timeout: 5), "Focused \(tv.focusedLabel)")
        return field
    }

    private func pressMenuUntilTabBar() {
        for _ in 0..<6 where !tv.isTabBarFocused {
            tv.remote.press(.menu)
            _ = waitForTabBarFocus()
        }
    }

    private func waitForTabBarFocus() -> Bool {
        let tabs = NSPredicate { _, _ in self.tv.isTabBarFocused }
        let expectation = XCTNSPredicateExpectation(predicate: tabs, object: nil)
        return XCTWaiter.wait(for: [expectation], timeout: 3) == .completed
    }

    private func waitForTab(_ title: String) -> Bool {
        let tab = NSPredicate { _, _ in self.tv.focusedTabTitle == title }
        let expectation = XCTNSPredicateExpectation(predicate: tab, object: nil)
        return XCTWaiter.wait(for: [expectation], timeout: 3) == .completed
    }

    private func focusFirstPoster() {
        for _ in 0..<6 where tv.isTabBarFocused || tv.isHeroFocused || isOnContinueWatching {
            tv.remote.press(tv.isTabBarFocused ? .right : .down)
        }
        XCTAssertFalse(tv.isTabBarFocused || tv.isHeroFocused || isOnContinueWatching)
    }

    private var isOnContinueWatching: Bool {
        tv.focusedIdentifier == TvApp.continueWatchingId
    }
}
