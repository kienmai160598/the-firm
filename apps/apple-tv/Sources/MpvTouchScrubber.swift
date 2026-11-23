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
    private static let settleBeforeSeek: Duration = .milliseconds(900)

    let preview = MpvScrubPreview()
    var panContext: () -> MpvPanContext? = { nil }
    var onScrubStart: () -> Void = {}
    var onSettle: () -> Void = {}
    private let clock: MpvPlaybackClock
    private let touchpad = MpvTouchpad()
    private var session: MpvScrubSession?
    private var settleTask: Task<Void, Never>?

    init(clock: MpvPlaybackClock) {
        self.clock = clock
    }

    var isPending: Bool { session != nil }

    var isSwiping: Bool { touchpad.isSwiping }

    var diagnosticRows: [MpvInfoRow] { touchpad.diagnostics.rows }

    func start() {
        touchpad.onMove = { [weak self] move in self?.follow(move) }
        touchpad.onRelease = { [weak self] velocity in self?.release(velocity) }
        touchpad.start()
    }

    func stop() {
        touchpad.stop()
        panContext = { nil }
        onScrubStart = {}
        onSettle = {}
        cancel()
    }

    func step(by steps: Int) {
        guard var current = session else { return }
        current.step(bySeconds: steps * Self.stepSeconds)
        session = current
        preview.settle(at: current.shownSeconds)
        scheduleSettle()
    }

    func commit() -> Int? {
        defer { cancel() }
        return session?.shownSeconds
    }

    func cancel() {
        settleTask?.cancel()
        settleTask = nil
        session = nil
        preview.show(nil)
    }

    private func follow(_ move: MpvTouchMove) {
        settleTask?.cancel()
        guard var current = session ?? begin() else { return }
        current.apply(move)
        session = current
        preview.show(current.shownSeconds)
    }

    private func begin() -> MpvScrubSession? {
        guard let context = panContext() else {
            touchpad.diagnostics.lastDecision = "no player"
            return nil
        }
        guard MpvPanResponse(context) == .scrub else {
            touchpad.diagnostics.lastDecision = context.isOnSurface ? "no length" : "menu open"
            return nil
        }
        touchpad.diagnostics.lastDecision = "scrub"
        onScrubStart()
        return MpvScrubSession(
            originSeconds: clock.positionSeconds,
            durationSeconds: clock.durationSeconds
        )
    }

    private func release(_ velocity: Float) {
        guard var current = session else { return }
        if !UIAccessibility.isReduceMotionEnabled {
            current.coast(releaseVelocity: velocity)
            session = current
        }
        preview.settle(at: current.shownSeconds)
        scheduleSettle()
    }

    private func scheduleSettle() {
        settleTask?.cancel()
        settleTask = Task { @MainActor [weak self] in
            try? await Task.sleep(for: Self.settleBeforeSeek)
            guard !Task.isCancelled else { return }
            self?.onSettle()
        }
    }
}
