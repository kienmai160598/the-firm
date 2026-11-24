import SwiftUI

/// The pages of the Settings tab, in the order the list shows them.
enum SettingsPage: String, CaseIterable, Hashable {
    case account
    case playback
    case subtitles
    case addons
    case about

    var title: String {
        switch self {
        case .account: "Account"
        case .playback: "Playback"
        case .subtitles: "Subtitles"
        case .addons: "Addons"
        case .about: "About"
        }
    }

    var symbol: String {
        switch self {
        case .account: "person.crop.circle"
        case .playback: "play.rectangle"
        case .subtitles: "captions.bubble"
        case .addons: "puzzlepiece.extension"
        case .about: "info.circle"
        }
    }

    var caption: String {
        switch self {
        case .account: "Sign in to Trakt to sync what you watch, your watchlist and your progress."
        case .playback: "The audio language a film starts in."
        case .subtitles: "The subtitles a film starts with, or none."
        case .addons: "Install, order and turn off the addons that bring catalogs, streams and subtitles."
        case .about: "Version, build and licence."
        }
    }

    @ViewBuilder
    var screen: some View {
        switch self {
        case .account: AccountView()
        case .playback: LanguageSettingsView(kind: .audio)
        case .subtitles: LanguageSettingsView(kind: .subtitles)
        case .addons: AddonsView()
        case .about: AboutView()
        }
    }
}

/// A page's symbol as the preview art, like the device picture of the Settings app.
struct SettingsSymbolArt: View {
    let symbol: String

    var body: some View {
        Image(systemName: symbol)
            .font(.system(size: 220))
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(.quaternary, in: RoundedRectangle(cornerRadius: SettingsMetrics.artRadius, style: .continuous))
    }
}
