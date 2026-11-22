import SwiftUI

struct MpvTransportBar: View {
    private static let restHeight: CGFloat = 12
    private static let scrubHeight: CGFloat = 18
    private static let trackSlot: CGFloat = 18
    private static let labelSlot: CGFloat = 180
    private static let labelRowHeight: CGFloat = 44
    private static let playheadRise: CGFloat = 10

    private static let skipRise: CGFloat = 40

    @ObservedObject var controller: MpvPlaybackController
    let isScrubbing: Bool
    let skip: MpvSkip?

    var body: some View {
        GeometryReader { geometry in
            let width = geometry.size.width
            let playheadX = width * fraction(controller.positionSeconds)
            VStack(spacing: 12) {
                track(width: width)
                    .overlay {
                        if isScrubbing { playhead(at: playheadX) }
                    }
                    .overlay {
                        if let skip { skipBadge(skip, at: playheadX, width: width) }
                    }
                labels(width: width, playheadX: playheadX)
            }
        }
        .frame(height: Self.trackSlot + 12 + Self.labelRowHeight)
        .animation(.easeOut(duration: 0.2), value: isScrubbing)
        .animation(.smooth(duration: 0.25), value: skip)
    }

    private func skipBadge(_ skip: MpvSkip, at x: CGFloat, width: CGFloat) -> some View {
        let margin = MpvSkipBadge.size / 2
        return MpvSkipBadge(skip: skip)
            .position(x: min(max(x, margin), width - margin), y: -Self.skipRise)
            .transition(.opacity.combined(with: .scale(scale: 0.7)))
    }

    private func track(width: CGFloat) -> some View {
        ZStack(alignment: .leading) {
            Capsule().fill(.thinMaterial)
            Capsule().fill(.white.opacity(0.3))
                .frame(width: width * fraction(controller.bufferedSeconds))
            Capsule().fill(.white.opacity(isScrubbing ? 0.55 : 0.9))
                .frame(width: width * fraction(controller.positionSeconds))
        }
        .overlay(Capsule().strokeBorder(.white.opacity(0.18), lineWidth: 1))
        .shadow(color: .black.opacity(0.3), radius: 6, y: 2)
        .frame(height: isScrubbing ? Self.scrubHeight : Self.restHeight)
        .frame(height: Self.trackSlot)
    }

    private func playhead(at x: CGFloat) -> some View {
        Capsule()
            .fill(.white)
            .frame(width: 4, height: Self.trackSlot + Self.playheadRise)
            .position(x: x, y: Self.trackSlot / 2)
    }

    private func labels(width: CGFloat, playheadX: CGFloat) -> some View {
        ZStack(alignment: .leading) {
            elapsed
                .frame(width: Self.labelSlot)
                .offset(x: clampedSlotOrigin(playheadX, width: width))
            HStack {
                Spacer()
                if controller.durationSeconds > 0 {
                    Text("-" + MpvClock.label(remainingSeconds))
                }
            }
        }
        .font(.system(size: 26, weight: .semibold).monospacedDigit())
        .foregroundStyle(.white)
        .frame(height: Self.labelRowHeight)
    }

    @ViewBuilder
    private var elapsed: some View {
        if controller.isPausedByViewer {
            Image(systemName: "pause.circle")
                .font(.system(size: 30, weight: .regular))
                .accessibilityLabel("Paused")
        } else {
            Text(MpvClock.label(controller.positionSeconds))
        }
    }

    private func clampedSlotOrigin(_ playheadX: CGFloat, width: CGFloat) -> CGFloat {
        let centered = playheadX - Self.labelSlot / 2
        return min(max(centered, 0), max(width - Self.labelSlot * 2, 0))
    }

    private var remainingSeconds: Int {
        max(controller.durationSeconds - controller.positionSeconds, 0)
    }

    private func fraction(_ seconds: Int) -> Double {
        guard controller.durationSeconds > 0 else { return 0 }
        return min(max(Double(seconds) / Double(controller.durationSeconds), 0), 1)
    }
}

struct MpvSkip: Equatable {
    let isForward: Bool
    let presses: Int

    func adding(isForward forward: Bool) -> MpvSkip {
        MpvSkip(isForward: forward, presses: forward == isForward ? presses + 1 : 1)
    }
}

private struct MpvSkipBadge: View {
    static let size: CGFloat = 64

    let skip: MpvSkip

    var body: some View {
        Image(systemName: skip.isForward ? "goforward.10" : "gobackward.10")
            .font(.system(size: 44, weight: .semibold))
            .foregroundStyle(.white)
            .shadow(color: .black.opacity(0.4), radius: 8)
            .symbolEffect(.bounce, value: skip.presses)
            .frame(width: Self.size, height: Self.size)
            .accessibilityLabel(skip.isForward ? "Skipped forward" : "Skipped back")
    }
}

enum MpvClock {
    static func label(_ totalSeconds: Int) -> String {
        let hours = totalSeconds / 3600
        let minutes = totalSeconds / 60 % 60
        let seconds = totalSeconds % 60
        guard hours > 0 else { return String(format: "%02d:%02d", minutes, seconds) }
        return String(format: "%d:%02d:%02d", hours, minutes, seconds)
    }
}
