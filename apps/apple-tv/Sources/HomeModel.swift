import Foundation
import NuvioTvShared

@MainActor
final class HomeModel: ObservableObject {
    @Published private(set) var continueWatching: TvosContinueWatchingRow?
    @Published private(set) var upcoming: TvosContinueWatchingRow?
    @Published private(set) var rows: [TvosRow] = []
    @Published private(set) var isLoading = true
    @Published private(set) var errorMessage: String?
    /// The Continue Watching card to focus next: the series' Next Up after one of its episodes is marked.
    @Published private(set) var continueWatchingFocus: String?

    private var subscription: TvosCancellable?
    private var markedSeries: MarkedSeries?

    func observe() {
        guard subscription == nil else { return }
        subscription = TvosHome.shared.observeHome { [weak self] state in
            self?.apply(state)
        }
    }

    func perform(_ action: ContinueWatchingAction, on item: TvosContinueWatchingItem) {
        switch action {
        case .markWatched:
            markedSeries = MarkedSeries(item: item)
            TvosHome.shared.markContinueWatchingWatched(key: item.key)
        case .remove:
            TvosHome.shared.removeFromContinueWatching(key: item.key)
        }
    }

    func continueWatchingFocusHandled() {
        continueWatchingFocus = nil
    }

    func stop() {
        subscription?.cancel()
        subscription = nil
    }

    private func apply(_ state: TvosHomeState) {
        continueWatching = state.continueWatching
        upcoming = state.upcoming
        rows = state.rows
        isLoading = state.isLoading
        errorMessage = state.errorMessage
        followMarkedSeries()
    }

    private func followMarkedSeries() {
        guard let marked = markedSeries else { return }
        guard Date() < marked.deadline else {
            markedSeries = nil
            return
        }
        guard let next = continueWatching?.items.first(where: marked.isNextCard) else { return }
        markedSeries = nil
        continueWatchingFocus = next.key
    }
}

/// A series whose Continue Watching episode was just marked watched: its Next Up card, when the
/// shared state brings one within a few seconds, takes focus instead of the hero.
private struct MarkedSeries {
    let seriesId: String
    let markedKey: String
    let deadline = Date().addingTimeInterval(10)

    init(item: TvosContinueWatchingItem) {
        seriesId = item.target.parentMetaId
        markedKey = item.key
    }

    func isNextCard(_ item: TvosContinueWatchingItem) -> Bool {
        item.key != markedKey && item.target.parentMetaId == seriesId
    }
}
