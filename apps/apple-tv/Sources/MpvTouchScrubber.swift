import UIKit

@MainActor
final class MpvScrubPreview: ObservableObject {
    @Published private(set) var seconds: Int?
    @Published private(set) var settleCount = 0

    func show(_ newSeconds: Int?) {
        if seconds != newSeconds { seconds = newSeconds }
    }

    func settle(at newSeconds: Int) {
        settleCount += 1
        show(newSeconds)
    }
}

@MainActor
final class MpvTouchScrubber {
    private static let stepSeconds = 10

    let preview = MpvScrubPreview()
    var canScrub: () -> Bool = { false }
    var onScrubStart: () -> Void = {}
    private let clock: MpvPlaybackClock
    private let touchpad = MpvTouchpad()
    private var session: MpvScrubSession?

    init(clock: MpvPlaybackClock) {
        self.clock = clock
    }

    var isPending: Bool { session != nil }

    var isSwiping: Bool { touchpad.isSwiping }

    func start() {
        touchpad.onMove = { [weak self] move in self?.follow(move) }
        touchpad.onRelease = { [weak self] velocity in self?.release(velocity) }
        touchpad.start()
    }

    func stop() {
        touchpad.stop()
        cancel()
    }

    func step(by steps: Int) {
        guard var current = session else { return }
        current.step(bySeconds: steps * Self.stepSeconds)
        session = current
        preview.settle(at: current.shownSeconds)
    }

    func commit() -> Int? {
        defer { cancel() }
        return session?.shownSeconds
    }

    func cancel() {
        session = nil
        preview.show(nil)
    }

    private func follow(_ move: MpvTouchMove) {
        guard var current = session ?? begin() else { return }
        current.apply(move)
        session = current
        preview.show(current.shownSeconds)
    }

    private func begin() -> MpvScrubSession? {
        guard clock.durationSeconds > 0, canScrub() else { return nil }
        onScrubStart()
        return MpvScrubSession(
            originSeconds: clock.positionSeconds,
            durationSeconds: clock.durationSeconds
        )
    }

    private func release(_ velocity: Float) {
        guard var current = session, !UIAccessibility.isReduceMotionEnabled else { return }
        current.coast(releaseVelocity: velocity)
        session = current
        preview.settle(at: current.shownSeconds)
    }
}
