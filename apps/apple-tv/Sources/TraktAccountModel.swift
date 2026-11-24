import Foundation
import NuvioTvShared

/// The Trakt account, kept in the shared Kotlin `TvosTrakt`.
@MainActor
final class TraktAccountModel: ObservableObject {
    @Published private(set) var state: TvosTraktState = TvosTrakt.shared.current()

    private var subscription: TvosCancellable?

    func observe() {
        guard subscription == nil else { return }
        subscription = TvosTrakt.shared.observe { [weak self] state in
            self?.state = state
        }
    }

    func stop() {
        subscription?.cancel()
        subscription = nil
    }

    func signIn() { TvosTrakt.shared.startSignIn() }
    func cancel() { TvosTrakt.shared.cancelSignIn() }
    func signOut() { TvosTrakt.shared.signOut() }

    /// The value the Settings list shows on the Account row.
    var summary: String {
        switch state.phase {
        case .signedIn: state.username ?? "Signed In"
        case .notConfigured: "Not Available"
        default: "Not Signed In"
        }
    }

    /// Why the last sign in stopped, in the words the page shows under the button.
    var failureText: String? {
        switch state.failure {
        case .expired: "The code expired before it was approved. Sign in again for a new one."
        case .denied: "Access was denied on Trakt."
        case .failed: "Trakt could not be reached. Check the network and try again."
        default: nil
        }
    }
}
