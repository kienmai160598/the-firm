import SwiftUI

struct MpvTransportBar: View {
    private static let restHeight: CGFloat = 12
    private static let scrubHeight: CGFloat = 18
    private static let trackSlot: CGFloat = 18
    private static let remainingReserve: CGFloat = 180
    private static let labelRowHeight: CGFloat = 44
    private static let playheadRise: CGFloat = 10
    private static let playheadWidth: CGFloat = 4
    private static let pauseIconSize: CGFloat = 30
    private static let skipGap: CGFloat = 16
    private static let skipGlyphGap: CGFloat = 8
    private static let progressAnimation: Animation = .smooth(duration: 0.3)
    private static let settleAnimation: Animation = .interactiveSpring(response: 0.25)

    @ObservedObject var clock: MpvPlaybackClock
    @ObservedObject var preview: MpvScrubPreview
    let isPausedByViewer: Bool
    let isScrubbing: Bool
    let skip: MpvSkip?
    @Environment(\.accessibilityReduceMotion) private var reducesMotion
    @State private var elapsedRowWidth: CGFloat = 0

    var body: some View {
        GeometryReader { geometry in
            let width = geometry.size.width
            let playheadX = width * clock.fraction(shownSeconds)
            VStack(spacing: 12) {
                track
                    .overlay(alignment: .leading) {
                        if isScrubbing || preview.seconds != nil { playhead(at: playheadX) }
                    }
                if clock.durationSeconds > 0 {
                    labels(width: width, playheadX: playheadX)
                }
            }
        }
        .frame(height: Self.trackSlot + 12 + Self.labelRowHeight)
        .animation(followsPlayback ? Self.progressAnimation : nil, value: clock.positionSeconds)
        .animation(reducesMotion ? nil : Self.settleAnimation, value: preview.settleCount)
        .animation(Self.progressAnimation, value: clock.bufferedSeconds)
        .animation(.easeOut(duration: 0.2), value: isScrubbing)
        .animation(.smooth(duration: 0.25), value: skip)
    }

    private var followsPlayback: Bool {
        preview.seconds == nil
    }

    private var shownSeconds: Int {
        preview.seconds ?? clock.positionSeconds
    }

    private var track: some View {
        ZStack {
            Capsule().fill(.thinMaterial)
            filled(clock.fraction(clock.bufferedSeconds), opacity: 0.3)
            filled(clock.fraction(clock.positionSeconds), opacity: isScrubbing ? 0.55 : 0.9)
        }
        .overlay(Capsule().strokeBorder(.white.opacity(0.18), lineWidth: 1))
        .shadow(color: .black.opacity(0.3), radius: 6, y: 2)
        .frame(height: isScrubbing ? Self.scrubHeight : Self.restHeight)
        .frame(height: Self.trackSlot)
    }

    private func filled(_ fraction: Double, opacity: Double) -> some View {
        Capsule()
            .fill(.white.opacity(opacity))
            .mask(alignment: .leading) {
                Rectangle().scaleEffect(x: fraction, anchor: .leading)
            }
    }

    private func playhead(at x: CGFloat) -> some View {
        Capsule()
            .fill(.white)
            .frame(width: Self.playheadWidth, height: Self.trackSlot + Self.playheadRise)
            .offset(x: x - Self.playheadWidth / 2)
    }

    private func labels(width: CGFloat, playheadX: CGFloat) -> some View {
        ZStack(alignment: .leading) {
            elapsedRow
                .fixedSize()
                .onGeometryChange(for: CGFloat.self) { proxy in
                    proxy.size.width
                } action: { measured in
                    elapsedRowWidth = measured
                }
                .offset(x: elapsedRowOrigin(playheadX, width: width))
            Text("-" + MpvClock.label(max(clock.durationSeconds - shownSeconds, 0)))
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .font(.system(size: 26, weight: .semibold).monospacedDigit())
        .contentTransition(.identity)
        .foregroundStyle(.white)
        .frame(height: Self.labelRowHeight)
    }

    private var showsPause: Bool {
        isPausedByViewer && preview.seconds == nil
    }

    /// The row under the playhead. At rest: the elapsed time, or the pause icon while paused,
    /// centred on the playhead. After a skip: the skip glyph and the time beside that anchor, on
    /// the right for forward and on the left for back, so nothing sits on top of anything else.
    private var elapsedRow: some View {
        HStack(spacing: Self.skipGap) {
            if let skip, !skip.isForward { skipped(skip) }
            if showsPause {
                Image(systemName: "pause.circle")
                    .font(.system(size: Self.pauseIconSize, weight: .regular))
                    .accessibilityLabel("Paused")
            }
            if skip == nil, !showsPause { Text(MpvClock.label(shownSeconds)) }
            if let skip, skip.isForward { skipped(skip) }
        }
    }

    private func skipped(_ skip: MpvSkip) -> some View {
        HStack(spacing: Self.skipGlyphGap) {
            MpvSkipBadge(skip: skip)
            Text(MpvClock.label(shownSeconds))
        }
        .transition(.opacity)
    }

    private func elapsedRowOrigin(_ playheadX: CGFloat, width: CGFloat) -> CGFloat {
        let anchorHalf = showsPause ? Self.pauseIconSize / 2 : 0
        let start: CGFloat
        switch skip?.isForward {
        case nil: start = playheadX - elapsedRowWidth / 2
        case true?: start = playheadX - anchorHalf
        case false?: start = playheadX + anchorHalf - elapsedRowWidth
        }
        let lastOrigin = width - Self.remainingReserve - elapsedRowWidth
        return min(max(start, 0), max(lastOrigin, 0))
    }
}

struct MpvSkip: Equatable {
    let isForward: Bool
    let presses: Int

    func adding(isForward forward: Bool) -> MpvSkip {
        MpvSkip(isForward: forward, presses: forward == isForward ? presses + 1 : 1)
    }
}

/// The "10" arrow of the direction, its number part of the glyph, bouncing on every press while
/// presses add up.
private struct MpvSkipBadge: View {
    let skip: MpvSkip

    var body: some View {
        Image(systemName: skip.isForward ? "goforward.10" : "gobackward.10")
            .font(.system(size: 30, weight: .semibold))
            .symbolEffect(.bounce, value: skip.presses)
            .accessibilityLabel(skip.isForward ? "Skipped forward" : "Skipped back")
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
