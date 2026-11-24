import SwiftUI

/// The app's version, build and licence, like the About page of Settings.
struct AboutView: View {
    private static let licence = "GPL-3.0"
    private static let credit = "Built on NuvioMobile and NuvioTV by NuvioMedia. Torrents play from memory through the Movies engine on libtorrent."

    var body: some View {
        SettingsSplit {
            SettingsPreview {
                SettingsSymbolArt(symbol: SettingsPage.about.symbol)
            } caption: {
                Text(SettingsPage.about.caption)
                    .foregroundStyle(.secondary)
            }
        } list: {
            List {
                Section {
                    LabeledContent("Name", value: bundleValue("CFBundleDisplayName") ?? "Movies")
                    LabeledContent("Version", value: bundleValue("CFBundleShortVersionString") ?? "")
                    LabeledContent("Build", value: bundleValue("CFBundleVersion") ?? "")
                    LabeledContent("Licence", value: Self.licence)
                } footer: {
                    Text(Self.credit)
                }
            }
        }
        .navigationTitle(SettingsPage.about.title)
    }

    private func bundleValue(_ key: String) -> String? {
        Bundle.main.object(forInfoDictionaryKey: key) as? String
    }
}
