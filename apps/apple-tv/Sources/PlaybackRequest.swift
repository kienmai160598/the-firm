import Foundation
import NuvioTvShared

struct PlaybackRequest: Identifiable {
    let id = UUID()
    let target: TvosStreamTarget
    let stream: TvosStream
    let url: URL
    let resumes: Bool

    init?(target: TvosStreamTarget, stream: TvosStream, resumes: Bool) {
        guard let playableUrl = stream.playableUrl, let url = URL(string: playableUrl) else {
            return nil
        }
        self.target = target
        self.stream = stream
        self.url = url
        self.resumes = resumes
    }
}

struct PendingResume: Identifiable {
    let stream: TvosStream
    let point: TvosResumePoint

    var id: String { stream.key }
}
