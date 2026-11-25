import NuvioTvShared

/// Decides the online subtitle to load as a film starts (`OnlineSubtitleChoice.startingChoice`)
/// once both the file's tracks and the online list are known, whichever comes last. It answers
/// once per playback, and not at all once the viewer has picked a subtitle.
@MainActor
final class MpvStartingSubtitle {
    private let preferred: [String]
    private var subtitles: [TvosSubtitle]?
    private var embeddedLanguages: [String]?
    private var isSettled = false

    init(preferred: [String]) {
        self.preferred = preferred
    }

    /// The URL of the subtitle to load now, if this list was the last thing missing.
    func online(_ subtitles: [TvosSubtitle]) -> String? {
        self.subtitles = subtitles
        return decide()
    }

    /// The URL of the subtitle to load now, if the file's tracks were the last thing missing.
    func tracks(languages: [String]) -> String? {
        embeddedLanguages = languages
        return decide()
    }

    func viewerChose() {
        isSettled = true
    }

    private func decide() -> String? {
        guard !isSettled, let subtitles, let embeddedLanguages else { return nil }
        isSettled = true
        let choice = OnlineSubtitleChoice.shared
        return choice.startingChoice(
            online: choice.inSourceOrder(subtitles: subtitles),
            preferred: preferred,
            embeddedLanguages: embeddedLanguages
        )?.url
    }
}
