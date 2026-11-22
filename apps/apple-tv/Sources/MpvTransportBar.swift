import SwiftUI

struct MpvTransportBar: View {
    @ObservedObject var controller: MpvPlaybackController
    let isFocused: Bool

    var body: some View {
        VStack(spacing: 14) {
            MpvScrubber(
                played: fraction(controller.positionSeconds),
                buffered: fraction(controller.bufferedSeconds),
                isFocused: isFocused
            )
            HStack {
                Image(systemName: controller.isPaused ? "pause.fill" : "play.fill")
                    .font(.caption)
                Text(MpvClock.label(controller.positionSeconds))
                Spacer()
                Text("-" + MpvClock.label(remainingSeconds))
            }
            .font(.callout.monospacedDigit().weight(.medium))
            .foregroundStyle(.white.opacity(0.85))
        }
    }

    private var remainingSeconds: Int {
        max(controller.durationSeconds - controller.positionSeconds, 0)
    }

    private func fraction(_ seconds: Int) -> Double {
        guard controller.durationSeconds > 0 else { return 0 }
        return min(max(Double(seconds) / Double(controller.durationSeconds), 0), 1)
    }
}

private struct MpvScrubber: View {
    let played: Double
    let buffered: Double
    let isFocused: Bool

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                Capsule().fill(.white.opacity(0.22))
                Capsule().fill(.white.opacity(0.4))
                    .frame(width: geometry.size.width * max(buffered, played))
                Capsule().fill(.white)
                    .frame(width: geometry.size.width * played)
            }
        }
        .frame(height: isFocused ? 12 : 8)
        .shadow(color: .white.opacity(isFocused ? 0.35 : 0), radius: 12)
        .animation(.easeOut(duration: 0.2), value: isFocused)
    }
}

enum MpvClock {
    static func label(_ totalSeconds: Int) -> String {
        let hours = totalSeconds / 3600
        let minutes = totalSeconds / 60 % 60
        let seconds = totalSeconds % 60
        guard hours > 0 else { return String(format: "%d:%02d", minutes, seconds) }
        return String(format: "%d:%02d:%02d", hours, minutes, seconds)
    }
}
