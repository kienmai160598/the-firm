import AVFoundation
import os

struct StreamNotice: Equatable {
    let title: String
    let message: String

    /// A Continue Watching card's last link did not play (AC H4): the list is the way on.
    static func lastLinkFailed(_ reason: StreamNotice?) -> StreamNotice {
        let message = ["Pick a source from the list.", reason?.message].compactMap { $0 }.joined(separator: " ")
        return StreamNotice(title: "The last link no longer plays", message: message)
    }
}

enum PlaybackPreflight {
    private static let probeTimeout: Duration = .seconds(6)
    /// Streams and audio AVPlayer always plays.
    private static let systemExtensions: Set<String> = [
        "m3u8", "m3u", "mp3", "m4a", "aac",
    ]
    /// Containers AVPlayer opens even when it cannot decode their video (HEVC tagged `hev1`, VP9,
    /// AV1): it then plays the sound over a black picture, so these are probed like an unknown link.
    private static let probedExtensions: Set<String> = ["mp4", "m4v", "mov"]
    private static let mpvExtensions: Set<String> = [
        "mkv", "avi", "webm", "flv", "wmv", "ogv", "rmvb", "divx", "mpd", "ts", "m2ts",
    ]
    private static let log = Logger(subsystem: "com.kien.movies.tvos", category: "Preflight")

    /// How a link picks its player before anything is loaded.
    enum Route: Equatable {
        case system
        case mpv
        case probe
    }

    static func engine(for request: PlaybackRequest) async -> PlayerEngine {
        let filenameExtension = request.stream.filename.map { ($0 as NSString).pathExtension }
        switch route(extensions: [request.url.pathExtension, filenameExtension].compactMap { $0 }) {
        case .mpv:
            log.notice("Chose mpv for \(request.url.pathExtension, privacy: .public)")
            return .mpv
        case .system:
            return .system
        case .probe:
            return await probe(PlaybackController.asset(for: request))
        }
    }

    /// The first extension the app knows decides; an unknown link, MP4 and MOV are probed.
    static func route(extensions: [String]) -> Route {
        let known = extensions.map { $0.lowercased() }.first { extensionRoutes[$0] != nil }
        return known.flatMap { extensionRoutes[$0] } ?? .probe
    }

    private static let extensionRoutes: [String: Route] =
        Dictionary(uniqueKeysWithValues: systemExtensions.map { ($0, Route.system) }
            + probedExtensions.map { ($0, Route.probe) }
            + mpvExtensions.map { ($0, Route.mpv) })

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
