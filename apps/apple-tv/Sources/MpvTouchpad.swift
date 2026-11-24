import GameController
import UIKit

@MainActor
final class MpvTouchpad: NSObject, UIGestureRecognizerDelegate {
    private static let swipeSettleTime: TimeInterval = 0.35

    var onMove: (MpvTouchMove) -> Void = { _ in }
    var onRelease: (Float) -> Void = { _ in }
    let diagnostics = MpvTouchDiagnostics()
    private var travel = MpvTouchTravel()
    private var lastPanTime: TimeInterval?
    private var recognizer: UIPanGestureRecognizer?
    private weak var window: UIWindow?
    private var countedPads: [GCMicroGamepad] = []

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
        GCController.controllers().forEach(countGameControllerTouches)
    }

    func stop() {
        recognizer.map { window?.removeGestureRecognizer($0) }
        recognizer = nil
        countedPads.forEach { $0.dpad.valueChangedHandler = nil }
        countedPads = []
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
        diagnostics.recordPan(sample)
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

    private func countGameControllerTouches(_ controller: GCController) {
        guard let pad = controller.microGamepad else { return }
        pad.dpad.valueChangedHandler = { [weak self] _, _, _ in
            MainActor.assumeIsolated { self?.diagnostics.recordGameControllerTouch() }
        }
        countedPads.append(pad)
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

@MainActor
final class MpvTouchDiagnostics {
    private(set) var panEvents = 0
    private(set) var gameControllerEvents = 0
    private(set) var widestTravel: Float = 0
    var lastDecision = "none"

    func recordPan(_ sample: MpvTouchSample) {
        panEvents += 1
        widestTravel = max(widestTravel, abs(sample.x))
    }

    func recordGameControllerTouch() {
        gameControllerEvents += 1
    }

    var rows: [MpvInfoRow] {
        [
            MpvInfoRow(label: "Swipe events", value: "\(panEvents)"),
            MpvInfoRow(label: "Remote touches", value: "\(gameControllerEvents)"),
            MpvInfoRow(label: "Widest swipe", value: String(format: "%.2f", widestTravel)),
            MpvInfoRow(label: "Swipe decision", value: lastDecision),
        ]
    }
}
