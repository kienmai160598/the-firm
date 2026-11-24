import UIKit

@MainActor
final class MpvTouchpad: NSObject, UIGestureRecognizerDelegate {
    private static let swipeSettleTime: TimeInterval = 0.35

    var onMove: (MpvTouchMove) -> Void = { _ in }
    var onRelease: (Float) -> Void = { _ in }
    private var travel = MpvTouchTravel()
    private var lastPanTime: TimeInterval?
    private var recognizer: UIPanGestureRecognizer?
    private weak var window: UIWindow?

    var isSwiping: Bool {
        guard !travel.isPanning else { return true }
        guard let lastPanTime else { return false }
        return Self.now - lastPanTime < Self.swipeSettleTime
    }

    func start() {
        guard recognizer == nil, let window = Self.keyWindow else { return }
        let pan = UIPanGestureRecognizer(target: self, action: #selector(panned))
        pan.allowedTouchTypes = [NSNumber(value: UITouch.TouchType.indirect.rawValue)]
        pan.cancelsTouchesInView = false
        pan.delegate = self
        window.addGestureRecognizer(pan)
        recognizer = pan
        self.window = window
    }

    func stop() {
        recognizer.map { window?.removeGestureRecognizer($0) }
        recognizer = nil
        travel = MpvTouchTravel()
    }

    nonisolated func gestureRecognizer(
        _ gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
    ) -> Bool {
        true
    }

    @objc private func panned(_ pan: UIPanGestureRecognizer) {
        let width = max(pan.view?.bounds.width ?? 1, 1)
        let translation = pan.translation(in: pan.view)
        let sample = MpvTouchSample(
            x: Float(translation.x / width) * MpvScrubGain.padWidth,
            y: Float(translation.y / width) * MpvScrubGain.padWidth,
            time: Self.now,
            isLifted: pan.state != .began && pan.state != .changed
        )
        track(sample)
    }

    private func track(_ sample: MpvTouchSample) {
        let wasPanning = travel.isPanning
        let releaseVelocity = travel.velocity
        if let move = travel.track(sample) {
            lastPanTime = sample.time
            onMove(move)
        } else if wasPanning, sample.isLifted {
            lastPanTime = sample.time
            onRelease(releaseVelocity)
        }
    }

    private static var keyWindow: UIWindow? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)
    }

    private static var now: TimeInterval {
        ProcessInfo.processInfo.systemUptime
    }
}
