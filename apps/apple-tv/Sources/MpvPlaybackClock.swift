import Foundation

@MainActor
final class MpvPlaybackClock: ObservableObject {
    @Published private(set) var positionSeconds = 0
    @Published private(set) var durationSeconds = 0
    @Published private(set) var bufferedSeconds = 0

    var remainingSeconds: Int {
        max(durationSeconds - positionSeconds, 0)
    }

    func fraction(_ seconds: Int) -> Double {
        guard durationSeconds > 0 else { return 0 }
        return min(max(Double(seconds) / Double(durationSeconds), 0), 1)
    }

    func setPosition(_ seconds: Int) {
        if positionSeconds != seconds { positionSeconds = seconds }
    }

    func setDuration(_ seconds: Int) {
        if durationSeconds != seconds { durationSeconds = seconds }
    }

    func setBuffered(_ seconds: Int) {
        if bufferedSeconds != seconds { bufferedSeconds = seconds }
    }
}
