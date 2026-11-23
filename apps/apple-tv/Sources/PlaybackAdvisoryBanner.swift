import SwiftUI

struct PlaybackAdvisoryBanner: View {
    private static let shownFor: Duration = .seconds(6)
    private static let horizontalInset: CGFloat = 84
    private static let topInset: CGFloat = 60

    let advisory: PlaybackAdvisory
    let hasPlaybackStarted: Bool
    @Environment(\.accessibilityReduceMotion) private var reducesMotion
    @State private var isShown = false

    var body: some View {
        ZStack(alignment: .topLeading) {
            if isShown {
                content
                    .transition(appearance)
            }
        }
        .padding(.leading, Self.horizontalInset)
        .padding(.top, Self.topInset)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .animation(.smooth(duration: 0.6), value: isShown)
        .allowsHitTesting(false)
        .task(id: hasPlaybackStarted) { await showOnce() }
    }

    private var appearance: AnyTransition {
        guard !reducesMotion else { return .opacity }
        return .opacity.combined(with: .offset(x: -16))
    }

    private var content: some View {
        HStack(alignment: .center, spacing: 18) {
            if let rating = advisory.ageRating {
                Text(rating)
                    .font(.system(size: 24, weight: .semibold))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 3)
                    .overlay(
                        RoundedRectangle(cornerRadius: 6)
                            .strokeBorder(.white.opacity(0.85), lineWidth: 2)
                    )
                    .accessibilityLabel("Rated \(rating)")
            }
            if let genreLine = advisory.genreLine {
                Text(genreLine)
                    .font(.system(size: 26, weight: .medium))
                    .lineLimit(1)
            }
        }
        .foregroundStyle(.white)
        .shadow(color: .black.opacity(0.6), radius: 10)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("playback-advisory")
    }

    private func showOnce() async {
        guard hasPlaybackStarted, !isShown else { return }
        isShown = true
        try? await Task.sleep(for: Self.shownFor)
        isShown = false
    }
}
