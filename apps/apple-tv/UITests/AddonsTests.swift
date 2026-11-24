import XCTest

final class AddonsTests: XCTestCase {
    private static let testAddonName = "Nuvio tvOS test films"
    private static let rowId = "addon-row"
    private static let qrId = "addonQrCode"
    private static let enabledId = "addon-enabled"

    private var tv: TvApp!

    override func setUp() {
        continueAfterFailure = false
        tv = TvApp(self)
    }

    func testAnAddonPageTurnsItOffInPlaceAndTheListSaysOff() throws {
        try XCTSkipUnless(TvApp.isAddonServerUp, "Test addon server is not running")
        tv.launch()
        openAddonsTab()
        XCTAssertTrue(tv.button(containing: Self.testAddonName).waitForExistence(timeout: 20))
        tv.screenshot("addons-list")

        openPage(of: Self.testAddonName)
        let provides = tv.app.descendants(matching: .any)
            .matching(NSPredicate(format: "label CONTAINS 'Streams'")).firstMatch
        XCTAssertTrue(provides.waitForExistence(timeout: 5), "No Provides row")
        let enabledFocused = NSPredicate(format: "label BEGINSWITH 'Enabled'")
        XCTAssertTrue(tv.waitUntil(tv.focused, enabledFocused, timeout: 3), "Focused \(tv.focusedLabel)")
        tv.screenshot("addons-page")
        tv.remote.press(.select)
        let turnedOff = NSPredicate(format: "label BEGINSWITH 'Enabled' AND label ENDSWITH 'Off'")
        XCTAssertTrue(tv.waitUntil(tv.focused, turnedOff, timeout: 3), "Focused \(tv.focusedLabel)")
        tv.remote.press(.menu)

        let offRow = tv.app.buttons.matching(
            NSPredicate(format: "label CONTAINS %@ AND label ENDSWITH 'Off'", Self.testAddonName)
        ).firstMatch
        XCTAssertTrue(offRow.waitForExistence(timeout: 5), "Focused \(tv.focusedLabel)")
        tv.screenshot("addons-turned-off")
    }

    func testMoveUpPutsTheAddonFirst() throws {
        try XCTSkipUnless(TvApp.isAddonServerUp, "Test addon server is not running")
        tv.launch()
        openAddonsTab()
        XCTAssertTrue(tv.button(containing: Self.testAddonName).waitForExistence(timeout: 20))
        openPage(of: Self.testAddonName)
        XCTAssertTrue(tv.app.buttons["Move Up"].firstMatch.waitForExistence(timeout: 5))

        focusButton(labeled: "Move Up", moving: .down)
        tv.remote.press(.select)
        tv.remote.press(.menu)

        let firstRow = tv.app.buttons.matching(identifier: Self.rowId).firstMatch
        let startsWithTestAddon = NSPredicate(format: "label BEGINSWITH %@", Self.testAddonName)
        XCTAssertTrue(tv.waitUntil(firstRow, startsWithTestAddon, timeout: 5), firstRow.label)
    }

    func testRemovingEveryAddonShowsTheEmptyState() {
        tv.launch(withTestAddon: false)
        openAddonsTab()
        let emptyState = tv.app.staticTexts["No addons"]

        for _ in 0..<4 where !emptyState.exists {
            removeFirstAddon()
        }

        XCTAssertTrue(emptyState.waitForExistence(timeout: 5))
        XCTAssertTrue(tv.waitForFocus(tv.app.textFields.firstMatch), "Focused \(tv.focusedLabel)")
        tv.screenshot("addons-empty")
    }

    func testAManifestThatCannotBeReachedShowsWhyItFailed() {
        tv.launch(withTestAddon: false)
        openAddonsTab()
        let field = tv.app.textFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        XCTAssertTrue(tv.waitForFocus(field), "Add Addon is not focused first: \(tv.focusedLabel)")
        tv.remote.press(.select)
        let keyboard = NSPredicate(format: "label == %@", "Keyboard")
        XCTAssertTrue(tv.waitUntil(tv.focused, keyboard, timeout: 5), "Focused \(tv.focusedLabel)")

        field.typeText("http://127.0.0.1:9\n")

        let failure = tv.app.staticTexts["Couldn't add this addon"]
        XCTAssertTrue(failure.waitForExistence(timeout: 30))
        tv.remote.press(.menu)
        XCTAssertTrue(tv.waitForFocus(field), "Focused \(tv.focusedLabel)")
        tv.screenshot("addons-install-failed")
    }

    /// The phone side of the QR flow, sent from the test runner on the same TV. Keeps the app
    /// state and sends an unreachable manifest, so it changes no addons on a real device.
    func testThePhonePageReachesTheTvAndGetsTheInstallResult() throws {
        tv.launch(keepingState: true, withTestAddon: false)
        openAddonsTab()
        let qrCode = tv.app.descendants(matching: .any)[Self.qrId]
        let pageUrl = try XCTUnwrap(URL(string: qrCode.value as? String ?? ""), "QR: \(qrCode.debugDescription)")
        tv.screenshot("addons-qr")

        let page = try fetch(URLRequest(url: pageUrl))
        XCTAssertEqual(page.status, 200)
        XCTAssertTrue(page.text.contains("Add to TV"), page.text)
        XCTAssertEqual(try fetch(URLRequest(url: pageUrl.withQuery("t=wrong"))).status, 403)

        var add = URLRequest(url: pageUrl.withPath("/add"))
        add.httpMethod = "POST"
        add.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        add.httpBody = Data("url=http%3A%2F%2F127.0.0.1%3A9".utf8)
        let reply = try fetch(add)

        XCTAssertEqual(reply.status, 422, reply.text)
        XCTAssertTrue(tv.app.staticTexts["Couldn't add this addon"].waitForExistence(timeout: 10))
    }

    private func fetch(_ request: URLRequest) throws -> (status: Int, text: String) {
        var result: Result<(Int, String), Error>?
        let done = expectation(description: request.url?.path ?? "")
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error {
                result = .failure(error)
            } else {
                let status = (response as? HTTPURLResponse)?.statusCode ?? 0
                result = .success((status, String(decoding: data ?? Data(), as: UTF8.self)))
            }
            done.fulfill()
        }.resume()
        wait(for: [done], timeout: 40)
        let (status, text) = try XCTUnwrap(result).get()
        return (status, text)
    }

    private func removeFirstAddon() {
        XCTAssertTrue(tv.app.buttons[Self.rowId].firstMatch.waitForExistence(timeout: 20))
        tv.remote.press(.down)
        XCTAssertTrue(waitForRowFocus(), "Focused \(tv.focusedLabel)")
        let name = tv.focusedLabel
        tv.remote.press(.select)
        XCTAssertTrue(tv.app.buttons["Remove Addon"].firstMatch.waitForExistence(timeout: 5))
        focusButton(labeled: "Remove Addon", moving: .down)
        tv.remote.press(.select)
        let question = tv.text(beginningWith: "Remove ")
        XCTAssertTrue(question.waitForExistence(timeout: 5), "No confirmation for \(name)")
        tv.screenshot("addons-remove-confirmation")
        focusButton(labeled: "Remove", moving: .right)
        tv.remote.press(.select)
        _ = tv.waitForDisappearance(question, timeout: 5)
        XCTAssertTrue(tv.app.textFields.firstMatch.waitForExistence(timeout: 5), "Did not return to the list")
    }

    /// Selects the row of `name` and waits for its page, where Enabled has focus.
    private func openPage(of name: String) {
        focusRow(containing: name)
        tv.remote.press(.select)
        XCTAssertTrue(tv.app.descendants(matching: .any)[Self.enabledId].waitForExistence(timeout: 5))
    }

    private func focusButton(labeled label: String, moving direction: XCUIRemote.Button) {
        for _ in 0..<5 where tv.focusedLabel != label {
            tv.remote.press(direction)
            _ = tv.waitUntil(tv.focused, NSPredicate(format: "label == %@", label), timeout: 1)
        }
        XCTAssertEqual(tv.focusedLabel, label)
    }

    private func waitForRowFocus() -> Bool {
        let cell = NSPredicate(format: "elementType == %d", XCUIElement.ElementType.cell.rawValue)
        return tv.waitUntil(tv.focused, cell, timeout: 3)
    }

    private func focusRow(containing name: String) {
        for _ in 0..<6 where !tv.focusedLabel.contains(name) {
            tv.remote.press(.down)
        }
        XCTAssertTrue(tv.focusedLabel.contains(name), "Focused \(tv.focusedLabel)")
    }

    private func openAddonsTab() {
        tv.openSettings(page: "Addons")
        XCTAssertTrue(tv.app.descendants(matching: .any)[Self.qrId].waitForExistence(timeout: 10))
    }
}

private extension URL {
    func withQuery(_ query: String) -> URL {
        var components = URLComponents(url: self, resolvingAgainstBaseURL: false)!
        components.query = query
        return components.url!
    }

    func withPath(_ path: String) -> URL {
        var components = URLComponents(url: self, resolvingAgainstBaseURL: false)!
        components.path = path
        return components.url!
    }
}
