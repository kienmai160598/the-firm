import Foundation
import NuvioTvShared

enum PlayerEngine {
    case system
    case mpv
}

struct PlaybackRequest: Identifiable {
    let id = UUID()
    let target: TvosStreamTarget
    let stream: TvosStream
    let url: URL
    let resumes: Bool
    let engine: PlayerEngine

    init?(
        target: TvosStreamTarget,
        stream: TvosStream,
        resumes: Bool,
        engine: PlayerEngine = .system
    ) {
        guard let playableUrl = stream.playableUrl, let url = URL(string: playableUrl) else {
            return nil
        }
        self.init(target: target, stream: stream, url: url, resumes: resumes, engine: engine)
    }

    init(
        target: TvosStreamTarget,
        stream: TvosStream,
        url: URL,
        resumes: Bool,
        engine: PlayerEngine = .system
    ) {
        self.target = target
        self.stream = stream
        self.url = url
        self.resumes = resumes
        self.engine = engine
    }
}

struct PendingResume: Identifiable {
    let stream: TvosStream
    let point: TvosResumePoint
    let engine: PlayerEngine

    var id: String { stream.key }
}
