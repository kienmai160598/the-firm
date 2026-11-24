import AVFoundation

/// Fades a trailer's sound in or out in small steps, so it never starts or stops with a jump.
@MainActor
enum TrailerVolume {
    private static let steps = 24

    static func ramp(_ player: AVPlayer, to target: Float, over seconds: Double) async {
        let start = player.volume
        let step = Duration.seconds(seconds / Double(steps))
        for index in 1...steps {
            try? await Task.sleep(for: step)
            guard !Task.isCancelled else { return }
            player.volume = start + (target - start) * Float(index) / Float(steps)
        }
    }
}
