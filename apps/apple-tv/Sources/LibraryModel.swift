import Foundation
import NuvioTvShared

@MainActor
final class LibraryModel: ObservableObject {
    @Published private(set) var rows: [TvosRow] = []
    @Published private(set) var isLoading = true
    @Published private(set) var errorMessage: String?

    private var subscription: TvosCancellable?

    func observe() {
        guard subscription == nil else { return }
        subscription = TvosLibrary.shared.observe { [weak self] state in
            self?.apply(state)
        }
    }

    func stop() {
        subscription?.cancel()
        subscription = nil
    }

    private func apply(_ state: TvosLibraryState) {
        rows = state.rows
        isLoading = state.isLoading
        errorMessage = state.errorMessage
    }
}
