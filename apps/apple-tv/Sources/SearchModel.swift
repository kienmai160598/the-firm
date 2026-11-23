import Foundation
import NuvioTvShared

@MainActor
final class SearchModel: ObservableObject {
    private static let typingPause: Duration = .milliseconds(350)

    @Published private(set) var rows: [TvosRow] = []
    @Published private(set) var isLoading = false
    @Published private(set) var message: String?

    private var subscription: TvosCancellable?
    private var pendingSearch: Task<Void, Never>?

    func observe() {
        guard subscription == nil else { return }
        subscription = TvosSearch.shared.observe { [weak self] state in
            self?.apply(state)
        }
    }

    func stop() {
        pendingSearch?.cancel()
        subscription?.cancel()
        subscription = nil
    }

    func queryChanged(_ query: String) {
        pendingSearch?.cancel()
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            TvosSearch.shared.clear()
            return
        }

        pendingSearch = Task {
            try? await Task.sleep(for: Self.typingPause)
            guard !Task.isCancelled else { return }
            TvosSearch.shared.search(query: trimmed)
        }
    }

    private func apply(_ state: TvosSearchState) {
        rows = state.rows
        isLoading = state.isLoading
        message = state.message
    }
}
