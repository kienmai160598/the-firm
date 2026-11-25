import XCTest

final class TvApp {
    static let addonUrl = "http://127.0.0.1:8765/manifest.json"
    static let tabTitles = ["Home", "Search", "Library", "Settings"]
    static let defaultMoves: [XCUIRemote.Button] = [
        .up, .up, .up, .down, .down, .down, .down, .right, .right,
    ]

    let app = XCUIApplication()
    let remote = XCUIRemote.shared
    private unowned let testCase: XCTestCase

    init(_ testCase: XCTestCase) {
        self.testCase = testCase
    }

    static var isAddonServerUp: Bool {
        guard let url = URL(string: addonUrl) else { return false }
        return (try? Data(contentsOf: url)) != nil
    }

    func launch(
        route: String? = nil,
        keepingState: Bool = false,
        withTestAddon: Bool = true,
        extra: [String] = []
    ) {
        app.terminate()
        let addon = withTestAddon ? ["-MoviesDebugAddonUrl", Self.addonUrl] : []
        let routeArguments = route.map { ["-MoviesDebugRoute", $0] } ?? []
        let state = keepingState ? [] : ["-MoviesDebugFreshState", "YES"]
        app.launchArguments = addon + routeArguments + state + extra
        app.launch()
    }

    var focused: XCUIElement {
        app.descendants(matching: .any)
            .matching(NSPredicate(format: "hasFocus == true"))
            .firstMatch
    }

    var focusedLabel: String {
        let current = focused
        return current.exists ? current.label : ""
    }

    var focusedIdentifier: String {
        let current = focused
        return current.exists ? current.identifier : ""
    }

    /// The player of either engine, AVPlayer or mpv.
    var player: XCUIElement {
        app.descendants(matching: .any)
            .matching(NSPredicate(format: "identifier IN %@", ["system-player", "mpv-player"]))
            .firstMatch
    }

    func button(beginningWith prefix: String) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", prefix)).firstMatch
    }

    func button(containing text: String) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label CONTAINS %@", text)).firstMatch
    }

    func text(beginningWith prefix: String) -> XCUIElement {
        app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", prefix)).firstMatch
    }

    func waitForFocus(_ element: XCUIElement, timeout: TimeInterval = 3) -> Bool {
        waitUntil(element, NSPredicate(format: "hasFocus == true"), timeout: timeout)
    }

    func waitForValue(_ element: XCUIElement, _ value: String, timeout: TimeInterval) -> Bool {
        waitUntil(element, NSPredicate(format: "value == %@", value), timeout: timeout)
    }

    func waitForDisappearance(_ element: XCUIElement, timeout: TimeInterval = 10) -> Bool {
        waitUntil(element, NSPredicate(format: "exists == false"), timeout: timeout)
    }

    func waitUntil(
        _ element: XCUIElement,
        _ predicate: NSPredicate,
        timeout: TimeInterval
    ) -> Bool {
        let expectation = XCTNSPredicateExpectation(predicate: predicate, object: element)
        return XCTWaiter.wait(for: [expectation], timeout: timeout) == .completed
    }

    func appearsBriefly(_ element: XCUIElement, within seconds: TimeInterval) -> Bool {
        let deadline = Date().addingTimeInterval(seconds)
        while Date() < deadline {
            if element.exists { return true }
        }
        return false
    }

    func watch(seconds: TimeInterval) {
        let playing = XCTestExpectation(description: "Playback runs for \(seconds) s")
        _ = XCTWaiter.wait(for: [playing], timeout: seconds)
    }

    func focus(
        _ element: XCUIElement,
        moving moves: [XCUIRemote.Button] = defaultMoves,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        for direction in moves where !element.hasFocus {
            remote.press(direction)
        }
        XCTAssertTrue(element.hasFocus, "Focused: \(focusedLabel)", file: file, line: line)
    }

    func select(_ element: XCUIElement, file: StaticString = #filePath, line: UInt = #line) {
        focus(element, file: file, line: line)
        remote.press(.select)
    }

    @discardableResult
    func pressMenu(until element: XCUIElement, attempts: Int = 4) -> Bool {
        for _ in 0..<attempts where !element.exists {
            remote.press(.menu)
            _ = element.waitForExistence(timeout: 5)
        }
        return element.exists
    }

    @discardableResult
    func pressMenu(untilFocused element: XCUIElement, attempts: Int = 5) -> Bool {
        for _ in 0..<attempts where !element.hasFocus {
            remote.press(.menu)
            _ = waitForFocus(element)
        }
        return element.hasFocus
    }

    func screenshot(_ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        testCase.add(attachment)
    }
}
