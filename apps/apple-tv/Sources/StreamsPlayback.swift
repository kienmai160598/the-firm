import Foundation
import NuvioTvShared

struct StreamSheet: Identifiable {
    let stream: TvosStream
    let notice: StreamNotice?

    var id: String { stream.key + (notice?.title ?? "") }
}

/// From a pressed row to the player: checks or starts the stream, asks where to start, and
/// brings back the info sheet when playback fails.
@MainActor
final class StreamsPlayback: ObservableObject {
    private static let playerPresentationTime: Duration = .seconds(1)

    @Published var sheet: StreamSheet?
    @Published var pendingResume: PendingResume?
    @Published var playback: PlaybackRequest?
    @Published private(set) var checkingStreamKey: String?
    @Published private(set) var directResume: TvosResumePoint?

    let torrent = TorrentSession()
    private var playbackFailure: StreamSheet?
    private let target: TvosStreamTarget
    private let resumesDirectly: Bool
    private var lastLink: StreamsLastLink?

    init(target: TvosStreamTarget, resumesDirectly: Bool) {
        self.target = target
        self.resumesDirectly = resumesDirectly
        lastLink = resumesDirectly ? StreamsLastLink(target: target) : nil
    }

    /// From Continue Watching: plays the link the title last played from, if any.
    func playLastLink() {
        guard let stream = lastLink?.take() else { return }
        select(stream)
    }

    func loadDirectResume() {
        guard resumesDirectly else { return }
        TvosPlayback.shared.resumePoint(target: target) { [weak self] point in
            self?.directResume = point
        }
    }

    func select(_ stream: TvosStream) {
        guard checkingStreamKey == nil else { return }
        if stream.playableUrl == nil, stream.infoHash != nil {
            startTorrent(stream)
            return
        }
        guard let probe = PlaybackRequest(target: target, stream: stream, resumes: false) else {
            sheet = StreamSheet(stream: stream, notice: notice(for: stream, nil))
            return
        }
        checkingStreamKey = stream.key
        preflight(probe)
    }

    func start(_ stream: TvosStream, resumes: Bool, engine: PlayerEngine) {
        pendingResume = nil
        let url = torrent.localUrl(for: stream)
        playback = url.map {
            PlaybackRequest(target: target, stream: stream, url: $0, resumes: resumes, engine: engine)
        } ?? PlaybackRequest(target: target, stream: stream, resumes: resumes, engine: engine)
        if playback == nil {
            checkingStreamKey = nil
        }
    }

    func clearSelectionOncePlayerCovers() async {
        try? await Task.sleep(for: Self.playerPresentationTime)
        checkingStreamKey = nil
    }

    func fail(_ request: PlaybackRequest, notice: StreamNotice) {
        checkingStreamKey = nil
        playbackFailure = StreamSheet(stream: request.stream, notice: self.notice(for: request.stream, notice))
        playback = nil
    }

    func afterPlayback() {
        checkingStreamKey = nil
        torrent.stop()
        loadDirectResume()
        guard let failure = playbackFailure else { return }
        playbackFailure = nil
        sheet = failure
    }

    func stop() {
        torrent.stop()
    }

    private func startTorrent(_ stream: TvosStream) {
        checkingStreamKey = stream.key
        torrent.start(stream) { [weak self] result in
            guard let self else { return }
            switch result {
            case let .success(url):
                preflight(PlaybackRequest(target: target, stream: stream, url: url, resumes: false))
            case let .failure(error):
                checkingStreamKey = nil
                let notice = StreamNotice(title: "The torrent did not start", message: error.message)
                sheet = StreamSheet(stream: stream, notice: self.notice(for: stream, notice))
            }
        }
    }

    private func notice(for stream: TvosStream, _ notice: StreamNotice?) -> StreamNotice? {
        lastLink?.notice(for: stream, notice) ?? notice
    }

    private func preflight(_ probe: PlaybackRequest) {
        Task { @MainActor in
            let engine = await PlaybackPreflight.engine(for: probe)
            chooseStart(probe.stream, engine: engine)
        }
    }

    private func chooseStart(_ stream: TvosStream, engine: PlayerEngine) {
        guard !resumesDirectly else {
            start(stream, resumes: true, engine: engine)
            return
        }
        TvosPlayback.shared.resumePoint(target: target) { [weak self] point in
            guard let self else { return }
            if let point {
                offerResume(PendingResume(stream: stream, point: point, engine: engine))
            } else {
                start(stream, resumes: false, engine: engine)
            }
        }
    }

    private func offerResume(_ pending: PendingResume) {
        #if DEBUG
        if let resumes = DebugLaunch.resumeChoice {
            start(pending.stream, resumes: resumes, engine: pending.engine)
            return
        }
        #endif
        checkingStreamKey = nil
        pendingResume = pending
    }
}
