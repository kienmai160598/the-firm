import SwiftUI

/// The Settings tab, laid out like the Settings app: the pages on the right, and on the left the
/// focused page's symbol and what it holds.
struct SettingsView: View {
    @StateObject private var languages = LanguageSettingsModel()
    @State private var previewed: SettingsPage = .playback
    @FocusState private var focus: SettingsPage?

    var body: some View {
        SettingsSplit(hasTitle: false) {
            SettingsPreview {
                SettingsSymbolArt(symbol: previewed.symbol)
            } caption: {
                Text(previewed.caption)
                    .foregroundStyle(.secondary)
            }
        } list: {
            List {
                ForEach(SettingsPage.allCases, id: \.self) { page in
                    RouteLink(value: .settings(page)) {
                        SettingsLinkLabel(title: page.title) {
                            if let value = value(of: page) { Text(value) }
                        }
                    }
                    .accessibilityLabel(label(of: page))
                    .accessibilityIdentifier("settings-\(page.rawValue)")
                    .focused($focus, equals: page)
                }
            }
        }
        .defaultFocus($focus, .playback)
        .onChange(of: focus) { _, focus in
            if let focus { previewed = focus }
        }
        .onAppear {
            languages.observe()
        }
        .onDisappear {
            languages.stop()
        }
    }

    private func value(of page: SettingsPage) -> String? {
        switch page {
        case .playback: LanguageName.of(languages.choice.audio)
        case .subtitles: LanguageName.of(languages.choice.subtitles)
        case .addons, .about: nil
        }
    }

    /// The row's title and value, spoken and read by the UI tests as "Subtitles, Off".
    private func label(of page: SettingsPage) -> String {
        [page.title, value(of: page)].compactMap { $0 }.joined(separator: ", ")
    }
}
