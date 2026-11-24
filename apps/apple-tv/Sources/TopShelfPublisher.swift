import NuvioTvShared
import TVServices
import os

/// Keeps the Top Shelf in step with Continue Watching while the app runs: writes the shelf to the
/// shared store and asks tvOS to reload the Top Shelf extension when it changed.
@MainActor
enum TopShelfPublisher {
    private static let log = Logger(subsystem: "com.kien.movies.tvos", category: "TopShelf")
    private static let maxItems = 10
    private static var subscription: TvosCancellable?

    static func start() {
        guard subscription == nil else { return }
        subscription = TvosHome.shared.observeHome { state in
            publish(state)
        }
    }

    private static func publish(_ state: TvosHomeState) {
        // Continue Watching reads empty until it loads: keep the last shelf until then.
        guard state.continueWatching != nil || !state.isLoading else { return }
        do {
            if try TopShelfStore.save(shelf(state.continueWatching)) {
                TVTopShelfContentProvider.topShelfContentDidChange()
            }
        } catch {
            log.error("Top Shelf save failed: \(error.localizedDescription, privacy: .public)")
        }
    }

    private static func shelf(_ row: TvosContinueWatchingRow?) -> TopShelfShelf? {
        guard let row, !row.items.isEmpty else { return nil }
        return TopShelfShelf(title: row.title, items: row.items.prefix(maxItems).map(item))
    }

    private static func item(_ item: TvosContinueWatchingItem) -> TopShelfItem {
        TopShelfItem(
            key: item.key,
            title: item.title,
            imageUrl: item.imageUrl.flatMap(URL.init(string:)),
            progress: item.isNextUp ? 0 : Double(item.progressFraction)
        )
    }
}

/// Resolves a Top Shelf link to its Continue Watching item. On a cold launch the item arrives a
/// moment after the app starts, so it waits for it, up to a limit.
@MainActor
final class ContinueWatchingLink {
    private static let timeout: Duration = .seconds(20)
    private var subscription: TvosCancellable?
    private var deadline: Task<Void, Never>?

    func open(key: String, onFound: @escaping (TvosStreamTarget) -> Void) {
        cancel()
        subscription = TvosHome.shared.observeHome { [weak self] state in
            let rows = [state.continueWatching, state.upcoming].compactMap { $0 }
            guard let item = rows.flatMap(\.items).first(where: { $0.key == key }) else { return }
            self?.cancel()
            onFound(item.target)
        }
        deadline = Task { [weak self] in
            try? await Task.sleep(for: Self.timeout)
            guard !Task.isCancelled else { return }
            self?.cancel()
        }
    }

    private func cancel() {
        subscription?.cancel()
        subscription = nil
        deadline?.cancel()
        deadline = nil
    }
}
