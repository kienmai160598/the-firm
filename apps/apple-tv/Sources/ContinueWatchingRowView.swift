import SwiftUI
import NuvioTvShared

/// What the card's context menu asks for. Mark as Watched and Remove take the card off the row;
/// Go to Details opens the title's Details and leaves the card where it is (W5).
enum ContinueWatchingAction {
    case goToDetails
    case markWatched
    case remove
}

extension TvosContinueWatchingItem {
    /// The show's Details for an episode card, the movie's for a movie card. Details opens on the
    /// season of its play action, which is this card's episode.
    var detailsRoute: AppRoute {
        .details(type: target.type, id: target.parentMetaId)
    }

    /// "Go to Show" or "Go to Movie", with the kind rule of `HomeHeroInfo`.
    var goToDetailsTitle: String {
        target.type == "series" ? "Go to Show" : "Go to Movie"
    }
}

struct ContinueWatchingRowView: View {
    let row: TvosContinueWatchingRow
    var focusRequest: String?
    var onFocusRequestHandled: () -> Void = {}
    let onAction: (TvosContinueWatchingItem, ContinueWatchingAction) -> Void
    @EnvironmentObject private var router: Router
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

    /// Details opens once focus is back on the card, so Back returns to it (N4). A card that
    /// leaves the row moves focus to its neighbour first instead of the sidebar.
    private func runPendingActionAfterMenuClosed() {
        guard let pending = pendingAction else { return }
        pendingAction = nil
        if case .goToDetails = pending.action {
            router.open(pending.item.detailsRoute)
            return
        }
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
