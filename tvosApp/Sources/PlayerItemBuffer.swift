import AVFoundation

extension AVPlayerItem {
    var bufferedEndSeconds: Double {
        loadedTimeRanges
            .map { $0.timeRangeValue.end.seconds }
            .filter(\.isFinite)
            .max() ?? 0
    }
}
