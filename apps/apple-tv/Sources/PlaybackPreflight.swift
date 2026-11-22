import AVFoundation
import os

struct StreamNotice: Equatable {
    let title: String
    let message: String
}

enum PlaybackPreflight {
    private static let probeTimeout: Duration = .seconds(6)
    private static let systemExtensions: Set<String> = [
        "mp4", "m4v", "mov", "m3u8", "m3u", "mp3", "m4a", "aac",
    ]
    private static let mpvExtensions: Set<String> = [
        "mkv", "avi", "webm", "flv", "wmv", "ogv", "rmvb", "divx", "mpd", "ts", "m2ts",
    ]
    private static let log = Logger(subsystem: "com.kien.movies.tvos", category: "Preflight")

    static func engine(for request: PlaybackRequest) async -> PlayerEngine {
        switch knownExtension(of: request) {
        case let .some(fileExtension) where mpvExtensions.contains(fileExtension):
            log.notice("Chose mpv for .\(fileExtension, privacy: .public)")
            return .mpv
        case .some:
            return .system
        case .none:
            return await probe(PlaybackController.asset(for: request))
        }
    }

    private static func knownExtension(of request: PlaybackRequest) -> String? {
        let filenameExtension = request.stream.filename.map { ($0 as NSString).pathExtension }
        return [request.url.pathExtension, filenameExtension]
            .compactMap { $0?.lowercased() }
            .first { systemExtensions.contains($0) || mpvExtensions.contains($0) }
    }

    private static func probe(_ asset: AVURLAsset) async -> PlayerEngine {
        let engine = await withCheckedContinuation { continuation in
            let firstEngine = FirstEngine(continuation)
            Task { firstEngine.resume(with: await loadEngine(asset)) }
            Task {
                try? await Task.sleep(for: probeTimeout)
                firstEngine.resume(with: nil)
            }
        }
        guard let engine else {
            asset.cancelLoading()
            log.notice("Probe timed out, choosing mpv")
            return .mpv
        }
        return engine
    }

    private static func loadEngine(_ asset: AVURLAsset) async -> PlayerEngine {
        do {
            let isPlayable = try await asset.load(.isPlayable)
            log.notice("Probe isPlayable=\(isPlayable, privacy: .public)")
            return isPlayable ? .system : .mpv
        } catch {
            log.notice("Probe failed, choosing mpv: \(error.localizedDescription, privacy: .public)")
            return .mpv
        }
    }

    static func readable(_ error: Error) -> String {
        let nsError = error as NSError
        return [nsError.localizedDescription, nsError.localizedFailureReason]
            .compactMap { $0 }
            .map { $0.hasSuffix(".") ? $0 : $0 + "." }
            .joined(separator: " ")
    }
}

private final class FirstEngine: Sendable {
    private let continuation: OSAllocatedUnfairLock<CheckedContinuation<PlayerEngine?, Never>?>

    init(_ continuation: CheckedContinuation<PlayerEngine?, Never>) {
        self.continuation = OSAllocatedUnfairLock(initialState: continuation)
    }

    func resume(with engine: PlayerEngine?) {
        let pending = continuation.withLock { state in
            defer { state = nil }
            return state
        }
        pending?.resume(returning: engine)
    }
}
