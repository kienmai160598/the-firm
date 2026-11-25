import NuvioTvShared
import SwiftUI

/// A Continue Watching or Upcoming card as in the Apple TV app: the artwork only, with the title
/// logo and one line (play glyph, progress, "S1, E1 · 17m") on a shade at its foot. The width is
/// the tvOS five-column grid preset.
struct ContinueWatchingCard: View {
    static let width: CGFloat = 320
    static let markWatchedTitle = "Mark as Watched"
    static let removeTitle = "Remove from Continue Watching"

    let item: TvosContinueWatchingItem
    let onAction: (TvosContinueWatchingItem, ContinueWatchingAction) -> Void

    var body: some View {
        RouteLink(value: AppRoute.streams(item.target, resumesDirectly: true)) {
            ContinueWatchingArtwork(item: item)
        }
        .buttonStyle(.borderless)
        .cardButtonShape()
        .contextMenu {
            Button(item.goToDetailsTitle, systemImage: "info.circle") {
                onAction(item, .goToDetails)
            }
            if item.canMarkWatched {
                Button(Self.markWatchedTitle, systemImage: "checkmark.circle") {
                    onAction(item, .markWatched)
                }
            }
            Button(Self.removeTitle, systemImage: "minus.circle", role: .destructive) {
                onAction(item, .remove)
            }
        }
        .accessibilityIdentifier("continueWatchingCard")
        .accessibilityLabel(item.title)
        .accessibilityValue(item.progressLabel ?? "")
    }
}

private struct ContinueWatchingArtwork: View {
    private static let height = ContinueWatchingCard.width * 9 / 16
    private static let logoSize = CGSize(width: 190, height: 56)
    private static let progressWidth: CGFloat = 38
    private static let progressHeight: CGFloat = 5

    let item: TvosContinueWatchingItem
    @Environment(\.isFocused) private var isFocused

    var body: some View {
        ArtworkImage(url: item.imageUrl)
            .frame(width: ContinueWatchingCard.width, height: Self.height)
            .overlay(alignment: .bottomLeading) { foot }
            .cardArtworkShape()
    }

    private var foot: some View {
        VStack(alignment: .leading, spacing: 8) {
            TitleLogoImage(url: item.target.logoUrl, maxSize: Self.logoSize) {
                Text(item.title)
                    .font(.system(size: 25, weight: .bold))
                    .lineLimit(1)
            }
            captionLine
                .foregroundStyle(.white.opacity(isFocused ? 1 : 0.72))
        }
        .padding(16)
        .frame(width: ContinueWatchingCard.width, height: Self.height, alignment: .bottomLeading)
        .background(alignment: .bottom) { shade }
    }

    private var captionLine: some View {
        HStack(spacing: 10) {
            Image(systemName: "play.fill")
                .font(.system(size: 17, weight: .semibold))
            if hasProgress {
                WatchProgressBar(
                    fraction: Double(item.progressFraction),
                    track: .white.opacity(0.3),
                    height: Self.progressHeight
                )
                .frame(width: Self.progressWidth)
            }
            Text(item.caption)
                .font(.system(size: 23, weight: .medium))
                .lineLimit(1)
        }
    }

    private var hasProgress: Bool {
        !item.isNextUp && item.progressFraction > 0
    }

    private var shade: some View {
        LinearGradient(
            stops: [
                .init(color: .clear, location: 0),
                .init(color: .black.opacity(0.55), location: 0.55),
                .init(color: .black.opacity(0.8), location: 1),
            ],
            startPoint: .top,
            endPoint: .bottom
        )
        .frame(height: Self.height * 0.62)
    }
}
