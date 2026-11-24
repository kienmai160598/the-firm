import Foundation
import NuvioTvShared

@MainActor
final class DetailsModel: ObservableObject {
    @Published private(set) var details: TvosMetaDetails?
    @Published private(set) var isLoading = true
    @Published private(set) var errorMessage: String?

    let type: String
    let id: String
    private var subscription: TvosCancellable?

    init(type: String, id: String) {
        self.type = type
        self.id = id
    }

    func observe() {
        guard subscription == nil else { return }
        subscription = TvosDetails.shared.observeDetails(type: type, id: id) { [weak self] state in
            self?.apply(state)
        }
    }

    func stop() {
        subscription?.cancel()
        subscription = nil
    }

    private func apply(_ state: TvosDetailsState) {
        if let loaded = state.details {
            details = loaded
        }
        isLoading = state.isLoading
        errorMessage = state.errorMessage
    }
}
