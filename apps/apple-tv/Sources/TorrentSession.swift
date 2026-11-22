import Foundation
import NuvioTvShared

@MainActor
final class TorrentSession: ObservableObject {
    @Published private(set) var status: TvosTorrentStatus?
    @Published private(set) var startingStreamKey: String?

    private var startCall: TvosCancellable?
    private var statusSubscription: TvosCancellable?
    private var localUrls: [String: URL] = [:]

    func localUrl(for stream: TvosStream) -> URL? {
        localUrls[stream.key]
    }

    func start(
        _ stream: TvosStream,
        completion: @escaping (Result<URL, TorrentStartError>) -> Void
    ) {
        cancelPendingStart()
        startingStreamKey = stream.key
        observeStatus()
        startCall = TvosTorrent.shared.start(stream: stream) { [weak self] result in
            self?.finishStart(stream, result: result, completion: completion)
        }
    }

    func stop() {
        cancelPendingStart()
        statusSubscription?.cancel()
        statusSubscription = nil
        status = nil
        localUrls.removeAll()
        TvosTorrent.shared.stop()
    }

    private func observeStatus() {
        guard statusSubscription == nil else { return }
        statusSubscription = TvosTorrent.shared.observe { [weak self] status in
            self?.status = status
        }
    }

    private func cancelPendingStart() {
        startCall?.cancel()
        startCall = nil
        startingStreamKey = nil
    }

    private func finishStart(
        _ stream: TvosStream,
        result: TvosTorrentStart,
        completion: (Result<URL, TorrentStartError>) -> Void
    ) {
        startCall = nil
        startingStreamKey = nil
        guard let localUrl = result.localUrl, let url = URL(string: localUrl) else {
            completion(.failure(TorrentStartError(message: result.errorMessage)))
            return
        }
        localUrls[stream.key] = url
        completion(.success(url))
    }
}

struct TorrentStartError: Error {
    private static let fallback = "The torrent could not be started."

    let message: String

    init(message: String?) {
        self.message = message ?? Self.fallback
    }
}
