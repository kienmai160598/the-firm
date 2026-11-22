import SwiftUI
import NuvioTvShared

struct ContinueWatchingRowView: View {
    let row: TvosContinueWatchingRow
    let onRemove: (TvosContinueWatchingItem) -> Void
    @FocusState private var focusedKey: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(row.title)
                .font(.title3.weight(.semibold))
                .padding(.horizontal, 80)
            ScrollView(.horizontal) {
                LazyHStack(alignment: .top, spacing: 48) {
                    ForEach(row.items, id: \.key) { item in
                        ContinueWatchingCard(item: item, onRemove: onRemove)
                            .focused($focusedKey, equals: item.key)
                    }
                }
                .padding(.horizontal, 80)
                .padding(.vertical, 30)
            }
            .scrollClipDisabled()
        }
        .focusSection()
        .defaultFocus($focusedKey, row.items.first?.key, priority: .userInitiated)
    }
}

private struct ContinueWatchingCard: View {
    static let width: CGFloat = 480
    static let removeTitle = "Remove from Continue Watching"

    let item: TvosContinueWatchingItem
    let onRemove: (TvosContinueWatchingItem) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            RouteLink(value: AppRoute.streams(item.target, resumesDirectly: true)) {
                ArtworkImage(url: item.imageUrl)
                    .frame(width: Self.width, height: Self.width * 9 / 16)
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
                .font(.callout.bold())
                .lineLimit(1)
            Text(detailLine)
                .font(.subheadline)
                .foregroundStyle(.secondary)
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
