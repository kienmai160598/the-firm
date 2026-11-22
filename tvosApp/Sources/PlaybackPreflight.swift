import AVFoundation
import os

struct StreamNotice: Equatable {
    let title: String
    let message: String
}

enum PlaybackVerdict {
    case playable
    case unplayable(StreamNotice)
}

enum PlaybackPreflight {
    private static let probeTimeout: Duration = .seconds(6)
    private static let playableExtensions: Set<String> = [
        "mp4", "m4v", "mov", "m3u8", "m3u", "mp3", "m4a", "aac",
    ]
    private static let unplayableExtensions: Set<String> = [
        "mkv", "avi", "webm", "flv", "wmv", "ogv", "rmvb", "divx", "mpd",
    ]
    private static let noticeTitle = "Apple TV cannot play this stream"
    private static let supportedFormats =
        "Nuvio on Apple TV plays MP4, MOV and HLS (m3u8) streams. Pick another stream."
    private static let log = Logger(subsystem: "com.nuvio.tvos", category: "Preflight")

    static func check(_ request: PlaybackRequest) async -> PlaybackVerdict {
        switch knownExtension(of: request) {
        case let .some(fileExtension) where unplayableExtensions.contains(fileExtension):
            log.notice("Refused .\(fileExtension, privacy: .public) by extension")
            return .unplayable(formatNotice(fileExtension))
        case .some:
            return .playable
        case .none:
            return await probe(PlaybackController.asset(for: request))
        }
    }

    private static func knownExtension(of request: PlaybackRequest) -> String? {
        let filenameExtension = request.stream.filename.map { ($0 as NSString).pathExtension }
        return [request.url.pathExtension, filenameExtension]
            .compactMap { $0?.lowercased() }
            .first { playableExtensions.contains($0) || unplayableExtensions.contains($0) }
    }

    private static func probe(_ asset: AVURLAsset) async -> PlaybackVerdict {
        let verdict = await withCheckedContinuation { continuation in
            let firstVerdict = FirstVerdict(continuation)
            Task { firstVerdict.resume(with: await loadPlayable(asset)) }
            Task {
                try? await Task.sleep(for: probeTimeout)
                firstVerdict.resume(with: nil)
            }
        }
        guard let verdict else {
            asset.cancelLoading()
            log.notice("Probe timed out, letting the player try")
            return .playable
        }
        return verdict
    }

    private static func loadPlayable(_ asset: AVURLAsset) async -> PlaybackVerdict {
        do {
            let isPlayable = try await asset.load(.isPlayable)
            log.notice("Probe isPlayable=\(isPlayable, privacy: .public)")
            return isPlayable ? .playable : .unplayable(unknownFormatNotice)
        } catch {
            log.notice("Probe failed: \(error.localizedDescription, privacy: .public)")
            return .unplayable(StreamNotice(
                title: noticeTitle,
                message: "The stream could not be opened: \(readable(error))"
            ))
        }
    }

    private static func formatNotice(_ fileExtension: String) -> StreamNotice {
        StreamNotice(
            title: noticeTitle,
            message: "\(fileExtension.uppercased()) files are not supported. \(supportedFormats)"
        )
    }

    private static var unknownFormatNotice: StreamNotice {
        StreamNotice(
            title: noticeTitle,
            message: "The player does not support this stream's format. \(supportedFormats)"
        )
    }

    static func readable(_ error: Error) -> String {
        let nsError = error as NSError
        return [nsError.localizedDescription, nsError.localizedFailureReason]
            .compactMap { $0 }
            .map { $0.hasSuffix(".") ? $0 : $0 + "." }
            .joined(separator: " ")
    }
}

private final class FirstVerdict: Sendable {
    private let continuation: OSAllocatedUnfairLock<CheckedContinuation<PlaybackVerdict?, Never>?>

    init(_ continuation: CheckedContinuation<PlaybackVerdict?, Never>) {
        self.continuation = OSAllocatedUnfairLock(initialState: continuation)
    }

    func resume(with verdict: PlaybackVerdict?) {
        let pending = continuation.withLock { state in
            defer { state = nil }
            return state
        }
        pending?.resume(returning: verdict)
    }
}
