import XCTest

/// Hitches and CPU on the Streams screen: opening it, walking a long list, and starting a stream.
/// The debug fixture adds a stable list of rows below the addons installed on the TV, so these
/// tests keep the app state. Take numbers only from an optimized build on a device.
@MainActor
final class StreamsPerformanceTests: XCTestCase {
    private static let route = "streams/movie/tt1160419"
    private static let fillerRows = 120
    private static let rowsWalked = 20
    private static let listTimeout: TimeInterval = 90
    private static let startWatchTime: TimeInterval = 6

    private lazy var tv = TvApp(self)

    override func setUpWithError() throws {
        guard #available(tvOS 26.0, *) else {
            throw XCTSkip("XCTHitchMetric needs tvOS 26")
        }
    }

    func testOpeningTheStreamListDoesNotHitch() {
        measureOnce { [self] in
            launchStreams()
            startMeasuring()
            XCTAssertTrue(firstFiller.waitForExistence(timeout: Self.listTimeout))
            stopMeasuring()
        }
    }

    func testWalkingTheStreamListDoesNotHitch() {
        launchStreams()
        XCTAssertTrue(firstFiller.waitForExistence(timeout: Self.listTimeout))
        XCTAssertTrue(tv.waitForFocus(firstRow), "Focused: \(tv.focusedLabel)")
        tv.screenshot("streams-before-walk")

        measure(metrics: metrics, options: Self.options(automatic: true)) {
            walk(.down)
            walk(.up)
        }

        walk(.down)
        XCTAssertTrue(tv.focusedLabel.contains("Fixture Filler"), "Focused: \(tv.focusedLabel)")
        tv.screenshot("streams-after-walk")
    }

    func testStartingATorrentDoesNotHitch() {
        measureOnce { [self] in
            launchStreams()
            let torrent = tv.button(containing: "Fixture Torrent")
            XCTAssertTrue(torrent.waitForExistence(timeout: Self.listTimeout))
            XCTAssertTrue(tv.waitForFocus(firstRow), "Focused: \(tv.focusedLabel)")
            tv.focus(torrent, moving: [.down])
            startMeasuring()
            tv.remote.press(.select)
            tv.watch(seconds: Self.startWatchTime)
            stopMeasuring()
            tv.screenshot("streams-starting-torrent")
        }
    }

    private var firstRow: XCUIElement {
        tv.button(containing: "Fixture Debrid")
    }

    private var firstFiller: XCUIElement {
        tv.button(containing: "Fixture Filler 0")
    }

    private var metrics: [XCTMetric] {
        guard #available(tvOS 26.0, *) else { return [] }
        return [XCTHitchMetric(application: tv.app), XCTCPUMetric(application: tv.app)]
    }

    private func launchStreams() {
        tv.launch(
            route: Self.route,
            keepingState: true,
            withTestAddon: false,
            extra: [
                "-MoviesDebugStreamFixture", "YES",
                "-MoviesDebugStreamFixtureFillerRows", "\(Self.fillerRows)",
            ]
        )
    }

    private func measureOnce(_ block: () -> Void) {
        measure(metrics: metrics, options: Self.options(automatic: false), block: block)
    }

    private func walk(_ direction: XCUIRemote.Button) {
        for _ in 0..<Self.rowsWalked {
            tv.remote.press(direction)
        }
    }

    private static func options(automatic: Bool) -> XCTMeasureOptions {
        let options = XCTMeasureOptions()
        options.iterationCount = 3
        options.invocationOptions = automatic ? [] : [.manuallyStart, .manuallyStop]
        return options
    }
}
