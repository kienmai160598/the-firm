import Foundation
import NuvioTvShared

enum StreamsPhase: Equatable {
    case loading
    case list
    case empty
}

@MainActor
final class StreamsModel: ObservableObject {
    @Published private(set) var groups: [TvosStreamGroup] = []
    @Published private(set) var isLoading = true
    @Published private(set) var emptyState: TvosStreamsEmptyState?

    let target: TvosStreamTarget
    private var subscription: TvosCancellable?

    init(target: TvosStreamTarget) {
        self.target = target
    }

    var phase: StreamsPhase {
        if emptyState != nil { return .empty }
        return visibleGroups.isEmpty ? .loading : .list
    }

    var visibleGroups: [TvosStreamGroup] {
        groups.filter { !$0.streams.isEmpty || $0.errorMessage != nil }
    }

    var searchingAddonCount: Int {
        groups.filter { $0.isLoading && $0.streams.isEmpty }.count
    }

    func observe() {
        guard subscription == nil else { return }
        subscription = TvosStreams.shared.observeStreams(target: target) { [weak self] state in
            self?.apply(state)
        }
    }

    func reload() {
        TvosStreams.shared.reload(target: target)
    }

    func stop() {
        subscription?.cancel()
        subscription = nil
    }

    private func apply(_ state: TvosStreamsState) {
        groups = state.groups
        isLoading = state.isLoading
        emptyState = state.emptyState
        #if DEBUG
        applyDebugFixture()
        #endif
    }

    #if DEBUG
    private func applyDebugFixture() {
        guard DebugLaunch.usesStreamFixture, !isLoading else { return }
        groups = [StreamsDebugFixture.group] + groups.filter { !$0.streams.isEmpty }
        emptyState = nil
    }
    #endif
}
