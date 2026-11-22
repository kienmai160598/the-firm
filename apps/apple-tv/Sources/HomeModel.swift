import Foundation
import NuvioTvShared

@MainActor
final class HomeModel: ObservableObject {
    @Published private(set) var continueWatching: TvosContinueWatchingRow?
    @Published private(set) var upcoming: TvosContinueWatchingRow?
    @Published private(set) var rows: [TvosRow] = []
    @Published private(set) var isLoading = true
    @Published private(set) var errorMessage: String?

    private var subscription: TvosCancellable?

    func observe() {
        guard subscription == nil else { return }
        subscription = TvosHome.shared.observeHome { [weak self] state in
            self?.apply(state)
        }
    }

    func removeFromContinueWatching(_ item: TvosContinueWatchingItem) {
        TvosHome.shared.removeFromContinueWatching(key: item.key)
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
    }
}
