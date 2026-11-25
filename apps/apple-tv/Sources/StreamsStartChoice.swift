import NuvioTvShared

/// Where a pressed stream starts: at the resume point without asking when Streams was opened
/// from Continue Watching (AC H4), from the beginning when there is none, otherwise the user
/// picks Resume or Start Over (AC S5).
enum StreamsStartChoice {
    case start(resumes: Bool)
    case ask(TvosResumePoint)

    @MainActor
    static func choose(
        target: TvosStreamTarget,
        resumesDirectly: Bool,
        completion: @escaping (StreamsStartChoice) -> Void
    ) {
        guard !resumesDirectly else { return completion(.start(resumes: true)) }
        TvosPlayback.shared.resumePoint(target: target) { point in
            guard let point else { return completion(.start(resumes: false)) }
            #if DEBUG
            if let resumes = DebugLaunch.resumeChoice { return completion(.start(resumes: resumes)) }
            #endif
            completion(.ask(point))
        }
    }
}
