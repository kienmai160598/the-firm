import XCTest

final class RemoteNavigationTests: XCTestCase {
    private let app = XCUIApplication()
    private let remote = XCUIRemote.shared

    override func setUp() {
        continueAfterFailure = false
    }

    func testPosterOpensDetailsAndPlayOpensStreams() {
        app.launch()
        XCTAssertTrue(app.staticTexts["Popular - Movies"].waitForExistence(timeout: 60))
        focusFirstPoster()
        saveScreenshot("ui-1-home-focused")

        remote.press(.select)
        XCTAssertTrue(playButton.waitForExistence(timeout: 30))
        saveScreenshot("ui-2-details")

        focusPlayButton()
        remote.press(.select)
        let streamsLabel = app.staticTexts["Streams"]
        XCTAssertTrue(streamsLabel.waitForExistence(timeout: 30))
        XCTAssertTrue(app.staticTexts["Playback unavailable"].waitForExistence(timeout: 30))
        saveScreenshot("ui-3-streams")

        remote.press(.menu)
        XCTAssertTrue(playButton.waitForExistence(timeout: 10))
        remote.press(.menu)
        XCTAssertTrue(app.staticTexts["Popular - Movies"].waitForExistence(timeout: 10))
    }

    func testHeroLeftChangesItemThenOpensSidebarAndDownReachesFirstRow() {
        app.launch()
        XCTAssertTrue(heroButton.waitForExistence(timeout: 60), "No hero")
        XCTAssertTrue(waitForFocus())
        focusHero()
        saveScreenshot("hero-1-focus")

        let firstTitle = heroTitle
        press(.right)
        XCTAssertTrue(isHeroFocused, "Right left the hero")
        XCTAssertNotEqual(heroTitle, firstTitle, "Right did not change the item")
        saveScreenshot("hero-2-second-item")
        press(.left)
        XCTAssertTrue(isHeroFocused, "Left on item 2 left the hero")
        XCTAssertEqual(heroTitle, firstTitle, "Left did not go back to item 1")

        press(.left)
        XCTAssertTrue(isTabBarFocused, "Left on item 1: \(focusedElement.label)")
        saveScreenshot("hero-3-sidebar")
        press(.right)
        XCTAssertTrue(isHeroFocused, "Right from sidebar: \(focusedElement.label)")
        XCTAssertEqual(heroTitle, firstTitle, "Right from sidebar changed the item")

        press(.down)
        XCTAssertFalse(isHeroFocused, "Down stayed on hero")
        XCTAssertFalse(isTabBarFocused)
        XCTAssertTrue(isContinueWatchingFocused, "Down from hero: \(focusedElement.label)")
        saveScreenshot("hero-4-first-row")
        press(.up)
        XCTAssertTrue(isHeroFocused, "Up from first row: \(focusedElement.label)")
    }

    func testSeasonPickerAndEpisodeCardOpenEpisodeStreams() {
        app.launchArguments = ["-MoviesDebugRoute", "details/series/tt0903747"]
        app.launch()
        XCTAssertTrue(playButton.waitForExistence(timeout: 60))
        XCTAssertTrue(app.staticTexts["E1  Pilot"].waitForExistence(timeout: 10))

        remote.press(.down)
        XCTAssertTrue(app.buttons["Season 1"].hasFocus)
        remote.press(.right)
        XCTAssertTrue(app.buttons["Season 2"].hasFocus)
        remote.press(.select)
        XCTAssertTrue(app.staticTexts["E1  Seven Thirty-Seven"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.staticTexts["E1  Pilot"].exists)
        saveScreenshot("ui-4-series-season-2")

        remote.press(.down)
        remote.press(.select)
        let episodeSubtitle = app.staticTexts
            .matching(NSPredicate(format: "label BEGINSWITH 'S2 E'"))
            .firstMatch
        XCTAssertTrue(episodeSubtitle.waitForExistence(timeout: 30))
        XCTAssertTrue(app.staticTexts["Playback unavailable"].waitForExistence(timeout: 30))
        saveScreenshot("ui-5-episode-streams")
    }

    func testFocusWalkKeepsFocusAndMenuGoesBackOneLevel() {
        app.launch()
        XCTAssertTrue(app.staticTexts["Popular - Movies"].waitForExistence(timeout: 60))
        focusFirstPoster()
        walkRow()
        let homeCard = focusedElement.label
        saveScreenshot("s6-6-walk-home-card-focus")

        remote.press(.select)
        XCTAssertTrue(playButton.waitForExistence(timeout: 30))
        XCTAssertTrue(waitForFocus(), "No focus on Details")
        press(.down)
        press(.up)
        focusPlayButton()
        remote.press(.select)
        XCTAssertTrue(app.staticTexts["Streams"].waitForExistence(timeout: 30))
        XCTAssertTrue(waitForFocus(), "No focus on Streams")
        saveScreenshot("s6-7-walk-streams-focus")

        remote.press(.menu)
        XCTAssertTrue(playButton.waitForExistence(timeout: 10))
        XCTAssertTrue(waitForFocus(), "No focus after Menu from Streams")
        XCTAssertFalse(app.staticTexts["Streams"].exists)
        remote.press(.menu)
        XCTAssertTrue(app.staticTexts["Popular - Movies"].waitForExistence(timeout: 10))
        XCTAssertTrue(waitForFocus(), "No focus after Menu from Details")
        XCTAssertEqual(focusedElement.label, homeCard, "Menu did not return to the opened card")
    }

    func testSeriesFocusWalkThroughSeasonsEpisodesAndBack() {
        app.launchArguments = ["-MoviesDebugRoute", "details/series/tt0903747"]
        app.launch()
        XCTAssertTrue(playButton.waitForExistence(timeout: 60))
        XCTAssertTrue(app.staticTexts["E1  Pilot"].waitForExistence(timeout: 10))

        press(.down)
        XCTAssertTrue(app.buttons["Season 1"].hasFocus, "Focused: \(focusedElement.label)")
        press(.right)
        press(.right)
        XCTAssertTrue(app.buttons["Season 3"].hasFocus, "Focused: \(focusedElement.label)")
        press(.left)
        XCTAssertTrue(app.buttons["Season 2"].hasFocus, "Focused: \(focusedElement.label)")
        remote.press(.select)
        XCTAssertTrue(app.staticTexts["E1  Seven Thirty-Seven"].waitForExistence(timeout: 10))
        saveScreenshot("s6-8-walk-season-picker")

        press(.down)
        let firstEpisode = focusedElement.identifier
        XCTAssertTrue(firstEpisode.hasPrefix("episode-"), "Focused: \(firstEpisode)")
        press(.right)
        let secondEpisode = focusedElement.identifier
        XCTAssertTrue(secondEpisode.hasPrefix("episode-"), "Focused: \(secondEpisode)")
        XCTAssertNotEqual(firstEpisode, secondEpisode)
        saveScreenshot("s6-9-walk-episode-card-focus")

        remote.press(.select)
        XCTAssertTrue(app.staticTexts["Streams"].waitForExistence(timeout: 30))
        XCTAssertTrue(waitForFocus(), "No focus on episode Streams")
        remote.press(.menu)
        XCTAssertTrue(playButton.waitForExistence(timeout: 10))
        XCTAssertTrue(waitForFocus(), "No focus after Menu from episode Streams")
        XCTAssertEqual(focusedElement.identifier, secondEpisode)
        XCTAssertTrue(app.staticTexts["E1  Seven Thirty-Seven"].exists, "Season 2 not kept")
        saveScreenshot("s6-10-walk-back-to-episode")

        press(.up)
        XCTAssertTrue(app.buttons["Season 2"].hasFocus, "Up from E2 did not land on Season 2")
        remote.press(.menu)
        XCTAssertTrue(app.staticTexts["Popular - Movies"].waitForExistence(timeout: 30))
        XCTAssertTrue(waitForFocus(), "No focus after Menu from series Details")
    }

    func testCastCardLiftsOnFocusAndEveryTabIsReachable() {
        app.launchArguments = ["-MoviesDebugRoute", "details/series/tt0903747"]
        app.launch()
        XCTAssertTrue(playButton.waitForExistence(timeout: 60))
        XCTAssertTrue(app.staticTexts["Cast & Crew"].waitForExistence(timeout: 10))
        for _ in 0..<4 where !isCastFocused {
            press(.down)
        }
        XCTAssertTrue(isCastFocused, "Focused: \(focusedElement.label)")
        press(.right)
        let castMember = focusedElement.label
        let liftedWidth = focusedElement.frame.width
        saveScreenshot("s6b-1-cast-card-focus")
        press(.left)
        let restingWidth = app.buttons[castMember].firstMatch.frame.width
        print("Cast \(castMember) width focused \(liftedWidth) resting \(restingWidth)")
        XCTAssertGreaterThan(liftedWidth, restingWidth + 10, "Focused cast card is not lifted")

        remote.press(.menu)
        XCTAssertTrue(app.staticTexts["Popular - Movies"].waitForExistence(timeout: 30))
        for _ in 0..<12 where !isTabBarFocused {
            remote.press(.left)
        }
        XCTAssertEqual(focusedTabTitle, "Home")
        for tab in ["Search", "Library", "Addons", "Settings"] {
            press(.down)
            XCTAssertEqual(focusedTabTitle, tab)
        }
        remote.press(.select)
        XCTAssertTrue(app.staticTexts["Coming in a later slice"].waitForExistence(timeout: 5))
        saveScreenshot("s6b-2-settings-tab")
        remote.press(.left)
        for _ in 0..<4 where focusedTabTitle != "Home" {
            press(.up)
        }
        XCTAssertEqual(focusedTabTitle, "Home")
        remote.press(.select)
        XCTAssertTrue(heroButton.waitForExistence(timeout: 10))
    }

    private var isCastFocused: Bool {
        let castTitle = app.staticTexts["Cast & Crew"].firstMatch
        return focusedElement.frame.minY > castTitle.frame.maxY
    }

    private func walkRow() {
        sleep(1)
        let startFrame = focusedElement.frame
        press(.right)
        press(.right)
        XCTAssertEqual(focusedElement.frame.midY, startFrame.midY, accuracy: 40, "Right left row")
        press(.left)
        let rowCard = focusedElement.label
        let liftedWidth = focusedElement.frame.width
        press(.down)
        XCTAssertNotEqual(focusedElement.label, rowCard, "Down did not move")
        let restingWidth = app.buttons[rowCard].firstMatch.frame.width
        print("Card \(rowCard) width focused \(liftedWidth) resting \(restingWidth)")
        XCTAssertGreaterThan(liftedWidth, restingWidth + 10, "Focused card is not lifted")
        press(.up)
        XCTAssertEqual(focusedElement.label, rowCard, "Up did not return to the same card")
    }

    private func press(_ button: XCUIRemote.Button) {
        let before = focusedElement.label
        remote.press(button)
        XCTAssertTrue(waitForFocus(), "Focus lost after \(button) from \(before)")
        print("Focus \(button): \(before) -> \(focusedElement.label) \(focusedElement.frame)")
    }

    private func waitForFocus() -> Bool {
        focusedElement.waitForExistence(timeout: 3)
    }

    private var playButton: XCUIElement {
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Play'")).firstMatch
    }

    private var heroButton: XCUIElement {
        app.buttons.matching(identifier: "homeHeroButton").firstMatch
    }

    private var isHeroFocused: Bool {
        focusedElement.identifier == "homeHeroButton"
    }

    private var heroTitle: String {
        focusedElement.value as? String ?? ""
    }

    private func focusHero() {
        for _ in 0..<4 where !isHeroFocused {
            remote.press(isTabBarFocused ? .right : .up)
        }
        XCTAssertTrue(isHeroFocused, "Focused: \(focusedElement.label)")
    }

    private var focusedElement: XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "hasFocus == true")).firstMatch
    }

    private func focusFirstPoster() {
        for _ in 0..<6 where isTabBarFocused || isContinueWatchingFocused || isHeroFocused {
            remote.press(isTabBarFocused ? .right : .down)
        }
        XCTAssertFalse(isTabBarFocused)
        XCTAssertFalse(isContinueWatchingFocused)
        XCTAssertFalse(isHeroFocused)
    }

    private var isContinueWatchingFocused: Bool {
        focusedElement.identifier == "continueWatchingCard"
    }

    private var isTabBarFocused: Bool {
        Self.tabTitles.contains(focusedTabTitle)
    }

    private static let tabTitles = ["Home", "Search", "Library", "Addons", "Settings"]

    private var focusedTabTitle: String {
        let focused = focusedElement
        guard focused.exists else { return "" }
        guard focused.label.isEmpty else { return focused.label }
        let title = focused.staticTexts.firstMatch
        return title.exists ? title.label : ""
    }

    private func focusPlayButton() {
        for _ in 0..<3 where !playButton.hasFocus {
            remote.press(.up)
        }
        XCTAssertTrue(playButton.hasFocus)
    }

    private func saveScreenshot(_ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
