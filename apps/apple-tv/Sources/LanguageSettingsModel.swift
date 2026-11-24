import Foundation
import NuvioTvShared

/// Which language a page picks: the audio a film starts in, or its subtitles.
enum LanguageKind {
    case audio
    case subtitles

    var page: SettingsPage {
        self == .audio ? .playback : .subtitles
    }

    var listTitle: String {
        self == .audio ? "Audio Language" : "Subtitle Language"
    }

    var choices: [String] {
        let settings = TvosSettings.shared
        let codes = self == .audio ? settings.audioChoices : settings.subtitleChoices
        let fixed = codes.prefix { LanguageName.isFixed($0) }
        let languages = codes.dropFirst(fixed.count).sorted { LanguageName.of($0) < LanguageName.of($1) }
        return Array(fixed) + languages
    }
}

/// The chosen languages, kept in the shared settings.
@MainActor
final class LanguageSettingsModel: ObservableObject {
    @Published private(set) var choice: TvosLanguageChoice = TvosSettings.shared.current()

    private var subscription: TvosCancellable?

    func observe() {
        guard subscription == nil else { return }
        subscription = TvosSettings.shared.observe { [weak self] choice in
            self?.choice = choice
        }
    }

    func stop() {
        subscription?.cancel()
        subscription = nil
    }

    func selected(_ kind: LanguageKind) -> String {
        kind == .audio ? choice.audio : choice.subtitles
    }

    func select(_ code: String, for kind: LanguageKind) {
        switch kind {
        case .audio: TvosSettings.shared.setAudio(code: code)
        case .subtitles: TvosSettings.shared.setSubtitles(code: code)
        }
    }
}

/// Names a language code in the app's language: "Automatic" and "Off" for the two choices that
/// are not a language.
enum LanguageName {
    static func isFixed(_ code: String) -> Bool {
        code == TvosPlayerLanguagesKt.TVOS_LANGUAGE_DEVICE || code == TvosPlayerLanguagesKt.TVOS_LANGUAGE_OFF
    }

    static func of(_ code: String) -> String {
        switch code {
        case TvosPlayerLanguagesKt.TVOS_LANGUAGE_DEVICE: "Automatic"
        case TvosPlayerLanguagesKt.TVOS_LANGUAGE_OFF: "Off"
        default: Locale.current.localizedString(forIdentifier: code) ?? code
        }
    }

    /// The device language for "Automatic", as a base code ("en" for "en-GB").
    static var device: String {
        Locale.current.language.languageCode?.identifier ?? "en"
    }
}
