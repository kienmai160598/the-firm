import XCTest
@testable import NuvioTv

final class MpvSwipeScrubTests: XCTestCase {
    func testTouchDownAndDeadZoneDoNotMoveThePlayhead() {
        var travel = MpvTouchTravel()

        let touch = travel.track(sample(x: -0.5, at: 0))
        let jitter = travel.track(sample(x: -0.45, y: 0.02, at: 0.02))
        let lock = travel.track(sample(x: -0.3, y: 0.02, at: 0.04))

        XCTAssertNil(touch)
        XCTAssertNil(jitter)
        XCTAssertEqual(lock, MpvTouchMove(delta: 0, velocity: 0, isPanStart: true))
        XCTAssertTrue(travel.isPanning)
    }

    func testLockedPanReportsHorizontalDeltasOnly() {
        var travel = lockedTravel()

        let move = travel.track(sample(x: -0.1, y: 0.4, at: 0.14))

        XCTAssertEqual(move?.delta ?? 0, 0.2, accuracy: 0.0001)
        XCTAssertGreaterThan(move?.velocity ?? 0, 0)
    }

    func testVerticalSwipeAndLiftDoNotPan() {
        var travel = MpvTouchTravel()

        _ = travel.track(sample(x: 0.1, y: -0.5, at: 0))
        let vertical = travel.track(sample(x: 0.3, y: 0.4, at: 0.05))
        let lifted = travel.track(sample(x: 0.3, y: 0.4, at: 0.1, isLifted: true))

        XCTAssertNil(vertical)
        XCTAssertNil(lifted)
        XCTAssertFalse(travel.isPanning)
    }

    func testPanScrubsOnTheVideoOnlyWhenTheLengthIsKnown() {
        let onVideo = MpvPanContext(isOnSurface: true, durationSeconds: 600)
        let inMenu = MpvPanContext(isOnSurface: false, durationSeconds: 600)
        let unknownLength = MpvPanContext(isOnSurface: true, durationSeconds: 0)

        XCTAssertEqual(MpvPanResponse(onVideo), .scrub)
        XCTAssertEqual(MpvPanResponse(inMenu), .ignore)
        XCTAssertEqual(MpvPanResponse(unknownLength), .ignore)
    }

    func testSlowSwipeMovesSecondsAndFastSwipeMovesMinutes() {
        let slow = MpvScrubGain.timeDelta(travel: 2, velocity: 0.3, durationSeconds: 7200)
        let fast = MpvScrubGain.timeDelta(travel: 2, velocity: 6, durationSeconds: 7200)
        let back = MpvScrubGain.timeDelta(travel: -1, velocity: -0.3, durationSeconds: 7200)

        XCTAssertEqual(slow, 20, accuracy: 0.001)
        XCTAssertEqual(fast, 600, accuracy: 0.001)
        XCTAssertEqual(back, -10, accuracy: 0.001)
    }

    func testFastGainShrinksForShortVideos() {
        let fast = MpvScrubGain.timeDelta(travel: 2, velocity: 6, durationSeconds: 180)

        XCTAssertEqual(fast, 30, accuracy: 0.001)
    }

    func testPreviewIsClampedToTheVideo() {
        var session = MpvScrubSession(originSeconds: 170, durationSeconds: 180)

        session.apply(MpvTouchMove(delta: 2, velocity: 6))
        let end = session.shownSeconds
        session.step(bySeconds: -400)

        XCTAssertEqual(end, 180)
        XCTAssertEqual(session.shownSeconds, 0)
    }

    func testCancelKeepsTheOriginAndCoastNeedsSpeed() {
        var session = MpvScrubSession(originSeconds: 600, durationSeconds: 7200)

        session.coast(releaseVelocity: 0.2)
        let afterSlowRelease = session.shownSeconds
        session.coast(releaseVelocity: 6)

        XCTAssertEqual(afterSlowRelease, 600)
        XCTAssertEqual(session.shownSeconds, 870)
        XCTAssertEqual(session.originSeconds, 600)
    }

    private func lockedTravel() -> MpvTouchTravel {
        var travel = MpvTouchTravel()
        _ = travel.track(sample(x: -0.5, at: 0))
        _ = travel.track(sample(x: -0.3, at: 0.04))
        return travel
    }

    private func sample(
        x: Float,
        y: Float = 0,
        at time: TimeInterval,
        isLifted: Bool = false
    ) -> MpvTouchSample {
        MpvTouchSample(x: x, y: y, time: time, isLifted: isLifted)
    }
}
