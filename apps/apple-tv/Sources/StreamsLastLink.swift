import NuvioTvShared

/// A Continue Watching card plays the link its title last played from, without the list (AC H4).
/// It is tried once per Streams screen. When it no longer plays, it is forgotten and the list
/// stays, with a notice saying so.
@MainActor
struct StreamsLastLink {
    static let failedTitle = "The last link no longer plays"
    static let failedAdvice = "Pick a source from the list."

    private let target: TvosStreamTarget
    private var streamKey: String?
    private var hasTried = false

    init(target: TvosStreamTarget) {
        self.target = target
    }

    /// The link to play now, the first time only; nil when none was played in the last day.
    mutating func take() -> TvosStream? {
        guard !hasTried else { return nil }
        hasTried = true
        let stream = TvosLastLink.shared.recall(target: target)
        streamKey = stream?.key
        return stream
    }

    /// The notice for a stream that did not play. For the last link it is forgotten, and the
    /// notice says the list is the way on.
    mutating func notice(for stream: TvosStream, _ notice: StreamNotice?) -> StreamNotice? {
        guard let streamKey, stream.key == streamKey else { return notice }
        self.streamKey = nil
        TvosLastLink.shared.forget(target: target)
        let message = [Self.failedAdvice, notice?.message].compactMap { $0 }.joined(separator: " ")
        return StreamNotice(title: Self.failedTitle, message: message)
    }
}
