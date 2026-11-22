import SwiftUI
import NuvioTvShared

struct ContinueWatchingRowView: View {
    let row: TvosContinueWatchingRow
    let onRemove: (TvosContinueWatchingItem) -> Void
    @FocusState private var focusedKey: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            ShelfHeader(title: row.title)
            ScrollView(.horizontal) {
                LazyHStack(alignment: .top, spacing: ShelfMetrics.spacing) {
                    ForEach(row.items, id: \.key) { item in
                        ContinueWatchingCard(item: item, onRemove: onRemove)
                            .focused($focusedKey, equals: item.key)
                    }
                }
                .padding(.horizontal, ShelfMetrics.inset)
                .padding(.vertical, 34)
            }
            .scrollClipDisabled()
        }
        .focusSection()
        .defaultFocus($focusedKey, row.items.first?.key, priority: .userInitiated)
    }
}

private struct ContinueWatchingCard: View {
    static let width: CGFloat = 400
    static let removeTitle = "Remove from Continue Watching"

    let item: TvosContinueWatchingItem
    let onRemove: (TvosContinueWatchingItem) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            RouteLink(value: AppRoute.streams(item.target, resumesDirectly: true)) {
                ArtworkImage(url: item.imageUrl)
                    .frame(width: Self.width, height: Self.width * 9 / 16)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .overlay(alignment: .bottom) {
                        if !item.isNextUp {
                            WatchProgressBar(fraction: Double(item.progressFraction))
                                .padding(20)
                        }
                    }
            }
            .buttonStyle(.card)
            .contextMenu {
                Button(Self.removeTitle, systemImage: "minus.circle", role: .destructive) {
                    onRemove(item)
                }
            }
            .accessibilityIdentifier("continueWatchingCard")
            .accessibilityLabel(item.title)
            .accessibilityValue(item.progressLabel ?? "")
            Text(item.title)
                .font(.system(size: 26, weight: .semibold))
                .lineLimit(1)
            Text(detailLine)
                .font(.system(size: 22))
                .foregroundStyle(.white.opacity(0.55))
                .lineLimit(1)
        }
        .frame(width: Self.width, alignment: .leading)
    }

    private var detailLine: String {
        [item.subtitle, item.progressLabel]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: "  ·  ")
    }
}

struct WatchProgressBar: View {
    let fraction: Double

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                Capsule().fill(.black.opacity(0.55))
                Capsule()
                    .fill(.white)
                    .frame(width: geometry.size.width * min(max(fraction, 0), 1))
            }
        }
        .frame(height: 8)
    }
}
