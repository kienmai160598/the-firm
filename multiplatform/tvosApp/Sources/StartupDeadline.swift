import AVFoundation
import os

@MainActor
final class StartupDeadline {
    static let limit: Duration = .seconds(20)
    private static let noData = "The stream sent no data for 20 seconds before it started playing."
    private static let log = Logger(subsystem: "com.kien.movies.tvos", category: "Player")

    private let item: AVPlayerItem
    private let onExpire: (Error) -> Void
    private var timer: Task<Void, Never>?

    init(item: AVPlayerItem, onExpire: @escaping (Error) -> Void) {
        self.item = item
        self.onExpire = onExpire
    }

    func start() {
        guard timer == nil else { return }
        timer = Task { [weak self] in await self?.expireWithoutProgress() }
    }

    func cancel() {
        timer?.cancel()
        timer = nil
    }

    private func expireWithoutProgress() async {
        var bufferedEnd = item.bufferedEndSeconds
        while !Task.isCancelled {
            try? await Task.sleep(for: Self.limit)
            guard !Task.isCancelled else { return }
            let latestEnd = item.bufferedEndSeconds
            Self.log.notice("Not playing yet, buffered to \(latestEnd, privacy: .public) s")
            guard latestEnd <= bufferedEnd else {
                bufferedEnd = latestEnd
                continue
            }
            timer = nil
            onExpire(Self.startupError)
            return
        }
    }

    private static var startupError: Error {
        NSError(
            domain: "com.kien.movies.tvos.playback",
            code: 2,
            userInfo: [NSLocalizedDescriptionKey: noData]
        )
    }
}
