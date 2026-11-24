import SwiftUI
import NuvioTvShared

/// What the card's context menu asks for. Both take the card off the row.
enum ContinueWatchingAction {
    case markWatched
    case remove
}

struct ContinueWatchingRowView: View {
    let row: TvosContinueWatchingRow
    var focusRequest: String?
    var onFocusRequestHandled: () -> Void = {}
    let onAction: (TvosContinueWatchingItem, ContinueWatchingAction) -> Void
    @FocusState private var focusedKey: String?
    @State private var pendingAction: (item: TvosContinueWatchingItem, action: ContinueWatchingAction)?

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            ShelfHeader(title: row.title)
            ScrollView(.horizontal) {
                LazyHStack(alignment: .top, spacing: ShelfMetrics.spacing) {
                    ForEach(row.items, id: \.key) { item in
                        ContinueWatchingCard(item: item) { pendingAction = ($0, $1) }
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
        .onChange(of: focusedKey) { _, key in
            if key != nil { runPendingActionAfterMenuClosed() }
        }
        .onChange(of: focusRequest, initial: true) { _, key in focus(requested: key) }
    }

    private func focus(requested key: String?) {
        guard let key, row.items.contains(where: { $0.key == key }) else { return }
        focusedKey = key
        onFocusRequestHandled()
    }

    /// The card leaves the row, so focus goes to its neighbour first instead of the sidebar.
    private func runPendingActionAfterMenuClosed() {
        guard let pending = pendingAction else { return }
        pendingAction = nil
        let keys = row.items.map(\.key)
        if let neighbor = RowNeighbor.key(beside: pending.item.key, in: keys) {
            focusedKey = neighbor
        }
        onAction(pending.item, pending.action)
    }
}

enum RowNeighbor {
    static func key(beside key: String, in keys: [String]) -> String? {
        guard let index = keys.firstIndex(of: key) else { return nil }
        let after = keys.index(after: index)
        if after < keys.endIndex { return keys[after] }
        return index > keys.startIndex ? keys[keys.index(before: index)] : nil
    }
}

private struct ContinueWatchingCard: View {
    static let width: CGFloat = 400
    static let markWatchedTitle = "Mark as Watched"
    static let removeTitle = "Remove from Continue Watching"

    let item: TvosContinueWatchingItem
    let onAction: (TvosContinueWatchingItem, ContinueWatchingAction) -> Void

    var body: some View {
        RouteLink(value: AppRoute.streams(item.target, resumesDirectly: true)) {
            artwork
            captions
        }
        .buttonStyle(.borderless)
        .cardButtonShape()
        .contextMenu {
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

    private var artwork: some View {
        ArtworkImage(url: item.imageUrl)
            .frame(width: Self.width, height: Self.width * 9 / 16)
            .overlay(alignment: .bottom) {
                if !item.isNextUp {
                    WatchProgressBar(fraction: Double(item.progressFraction))
                        .padding(CardShape.contentInset)
                }
            }
            .cardArtworkShape()
    }

    private var captions: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(item.title)
                .font(.system(size: 26, weight: .semibold))
                .lineLimit(1)
            FactLine(facts: details)
                .font(.system(size: 22))
                .foregroundStyle(.white.opacity(0.55))
                .lineLimit(1)
        }
        .frame(width: Self.width, alignment: .leading)
    }

    private var details: [String] {
        [item.subtitle, item.progressLabel]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
    }
}

struct WatchProgressBar: View {
    let fraction: Double
    var track: Color = .black.opacity(0.55)
    var height: CGFloat = 8

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                Capsule().fill(track)
                Capsule()
                    .fill(.white)
                    .frame(width: geometry.size.width * min(max(fraction, 0), 1))
            }
        }
        .frame(height: height)
    }
}
