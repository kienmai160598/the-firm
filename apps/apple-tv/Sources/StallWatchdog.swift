import AVFoundation
import os

@MainActor
final class StallWatchdog {
    private static let stallLimit: Duration = .seconds(20)
    private static let stoppedSendingData = "The stream stopped sending data."
    private static let log = Logger(subsystem: "com.kien.movies.tvos", category: "Player")

    private let item: AVPlayerItem
    private let onGiveUp: (Error) -> Void
    private var stallTimer: Task<Void, Never>?
    private var hasPlayed = false

    init(item: AVPlayerItem, onGiveUp: @escaping (Error) -> Void) {
        self.item = item
        self.onGiveUp = onGiveUp
    }

    func update(_ status: AVPlayer.TimeControlStatus) {
        switch status {
        case .waitingToPlayAtSpecifiedRate where hasPlayed:
            startWatchingOnce()
        case .playing:
            hasPlayed = true
            stop()
        case .paused, .waitingToPlayAtSpecifiedRate:
            stop()
        @unknown default:
            break
        }
    }

    func stop() {
        stallTimer?.cancel()
        stallTimer = nil
    }

    private func startWatchingOnce() {
        guard stallTimer == nil else { return }
        stallTimer = Task { [weak self] in await self?.watchStall() }
    }

    private func watchStall() async {
        var bufferedEnd = item.bufferedEndSeconds
        Self.log.notice("Stalled after playing, buffered to \(bufferedEnd, privacy: .public) s")
        while !Task.isCancelled {
            try? await Task.sleep(for: Self.stallLimit)
            guard !Task.isCancelled else { return }
            let latestEnd = item.bufferedEndSeconds
            Self.log.notice("Stalled, buffered to \(latestEnd, privacy: .public) s")
            guard latestEnd <= bufferedEnd else {
                bufferedEnd = latestEnd
                continue
            }
            stallTimer = nil
            onGiveUp(stallError)
            return
        }
    }

    private var stallError: Error {
        guard let lastEvent = item.errorLog()?.events.last else {
            return NSError(
                domain: "com.kien.movies.tvos.playback",
                code: 1,
                userInfo: [NSLocalizedDescriptionKey: Self.stoppedSendingData]
            )
        }
        guard lastEvent.errorDomain == NSURLErrorDomain else {
            return NSError(
                domain: lastEvent.errorDomain,
                code: lastEvent.errorStatusCode,
                userInfo: [NSLocalizedDescriptionKey: lastEvent.errorComment
                    ?? Self.stoppedSendingData]
            )
        }
        return URLError(URLError.Code(rawValue: lastEvent.errorStatusCode))
    }
}
