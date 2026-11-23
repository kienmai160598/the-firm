import GameController

@MainActor
final class MpvTouchpad {
    private static let swipeSettleTime: TimeInterval = 0.35

    var onMove: (MpvTouchMove) -> Void = { _ in }
    var onRelease: (Float) -> Void = { _ in }
    private var travel = MpvTouchTravel()
    private var lastPanTime: TimeInterval?
    private var connectObserver: NSObjectProtocol?
    private var attachedPads: [GCMicroGamepad] = []

    var isSwiping: Bool {
        guard !travel.isPanning else { return true }
        guard let lastPanTime else { return false }
        return Self.now - lastPanTime < Self.swipeSettleTime
    }

    func start() {
        guard connectObserver == nil else { return }
        GCController.controllers().forEach(attach)
        connectObserver = NotificationCenter.default.addObserver(
            forName: .GCControllerDidConnect,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            guard let controller = notification.object as? GCController else { return }
            MainActor.assumeIsolated { self?.attach(controller) }
        }
    }

    func stop() {
        connectObserver.map(NotificationCenter.default.removeObserver)
        connectObserver = nil
        for pad in attachedPads {
            pad.dpad.valueChangedHandler = nil
            pad.reportsAbsoluteDpadValues = false
        }
        attachedPads = []
        travel = MpvTouchTravel()
    }

    private func attach(_ controller: GCController) {
        guard let pad = controller.microGamepad,
              !attachedPads.contains(where: { $0 === pad })
        else { return }
        pad.reportsAbsoluteDpadValues = true
        pad.dpad.valueChangedHandler = { [weak self] _, x, y in
            MainActor.assumeIsolated {
                self?.track(MpvTouchSample(x: x, y: y, time: Self.now))
            }
        }
        attachedPads.append(pad)
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

    private static var now: TimeInterval {
        ProcessInfo.processInfo.systemUptime
    }
}
