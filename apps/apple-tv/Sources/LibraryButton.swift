import NuvioTvShared
import SwiftUI

/// The round Library button next to Play on Details: a plus when the title is not saved, a check
/// when it is. Where it saves (local list or Trakt watchlist) is `LibraryRepository`'s choice.
struct LibraryButton: View {
    @StateObject private var model: LibraryButtonModel

    init(type: String, id: String) {
        _model = StateObject(wrappedValue: LibraryButtonModel(type: type, id: id))
    }

    var body: some View {
        Button(action: { model.toggle(confirmRemoval: false) }) {
            Image(systemName: model.isSaved ? "checkmark" : "plus")
                .font(.system(size: 28, weight: .semibold))
                .frame(width: 66, height: 66)
        }
        .glassCircleButtonStyle()
        .disabled(model.isBusy)
        .accessibilityLabel(model.isSaved ? "In Library" : "Add to Library")
        .accessibilityIdentifier("details-library")
        .onAppear(perform: model.observe)
        .onDisappear(perform: model.stop)
        .confirmationDialog(
            "Remove from your Library?",
            isPresented: $model.isConfirmingRemoval,
            titleVisibility: .visible
        ) {
            Button("Remove", role: .destructive) { model.toggle(confirmRemoval: true) }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This also removes it from your Trakt list.")
        }
        .alert("Your Library could not be updated", isPresented: $model.hasFailed) {
            Button("OK", role: .cancel) {}
        }
    }
}

@MainActor
final class LibraryButtonModel: ObservableObject {
    @Published private(set) var isSaved = false
    @Published private(set) var isBusy = false
    @Published var isConfirmingRemoval = false
    @Published var hasFailed = false

    private let type: String
    private let id: String
    private var subscription: TvosCancellable?

    init(type: String, id: String) {
        self.type = type
        self.id = id
    }

    func observe() {
        guard subscription == nil else { return }
        subscription = TvosLibrary.shared.observeSaved(type: type, id: id) { [weak self] saved in
            self?.isSaved = saved.boolValue
        }
    }

    func stop() {
        subscription?.cancel()
        subscription = nil
    }

    func toggle(confirmRemoval: Bool) {
        guard !isBusy else { return }
        isBusy = true
        TvosLibrary.shared.toggle(type: type, id: id, confirmRemoval: confirmRemoval) { [weak self] outcome in
            self?.finish(outcome)
        }
    }

    private func finish(_ outcome: TvosLibraryToggle) {
        isBusy = false
        switch outcome {
        case .saved: isSaved = true
        case .removed: isSaved = false
        case .needsConfirmation: isConfirmingRemoval = true
        default: hasFailed = true
        }
    }
}
