import Foundation
import NuvioTvShared

enum StreamsPhase: Equatable {
    case loading
    case list
    case empty
}

@MainActor
final class StreamsModel: ObservableObject {
    @Published private(set) var items: [StreamListItem] = []
    @Published private(set) var sourceCount = 0
    @Published private(set) var searchingAddonCount = 0
    @Published private(set) var emptyState: TvosStreamsEmptyState?

    let target: TvosStreamTarget
    private var subscription: TvosCancellable?
    private var builder = StreamListBuilder()

    init(target: TvosStreamTarget) {
        self.target = target
    }

    var phase: StreamsPhase {
        if emptyState != nil { return .empty }
        return items.isEmpty ? .loading : .list
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
        var groups = state.groups
        var emptyState = state.emptyState
        #if DEBUG
        if DebugLaunch.usesStreamFixture, !state.isLoading {
            let fixture = StreamsDebugFixture.group(extraRows: DebugLaunch.streamFixtureFillerRows)
            groups = [fixture] + groups.filter { !$0.streams.isEmpty }
            emptyState = nil
        }
        #endif
        update(\.items, to: builder.items(for: groups))
        update(\.sourceCount, to: groups.reduce(0) { $0 + $1.streams.count })
        update(\.searchingAddonCount, to: groups.filter { $0.isLoading && $0.streams.isEmpty }.count)
        update(\.emptyState, to: emptyState)
    }

    /// Publishes only real changes, so an addon update that changes nothing redraws nothing.
    private func update<Value: Equatable>(
        _ keyPath: ReferenceWritableKeyPath<StreamsModel, Value>,
        to value: Value
    ) {
        if self[keyPath: keyPath] != value { self[keyPath: keyPath] = value }
    }
}
