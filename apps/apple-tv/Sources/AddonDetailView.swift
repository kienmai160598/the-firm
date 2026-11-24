import SwiftUI
import NuvioTvShared

/// One addon's page, like an app's page in Settings: Enabled turns it off in place, Move changes
/// its order, About lists what it is, and Remove asks first.
struct AddonDetailView: View {
    let manifestUrl: String

    @StateObject private var model = AddonsModel()

    @State private var isConfirmingRemoval = false
    @Environment(\.dismiss) private var dismiss

    private static let removalMessage = "Its catalogs, streams and subtitles leave the app."

    var body: some View {
        content
            .onAppear(perform: model.observe)
            .onDisappear(perform: model.stop)
    }

    @ViewBuilder
    private var content: some View {
        if let addon = model.addon(manifestUrl) {
            SettingsSplit {
                AddonSummary(addon: addon, showsDescription: false)
            } list: {
                rows(addon)
            }
            .navigationTitle(addon.name)
            .alert("Remove \(addon.name)?", isPresented: $isConfirmingRemoval) {
                Button("Remove", role: .destructive) { remove(addon) }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text(Self.removalMessage)
            }
        }
    }

    private func rows(_ addon: TvosAddon) -> some View {
        List {
            Toggle("Enabled", isOn: enabled(addon))
                .accessibilityIdentifier("addon-enabled")
            if model.canMove(addon, .up) {
                Button("Move Up") { model.move(addon, .up) }
            }
            if model.canMove(addon, .down) {
                Button("Move Down") { model.move(addon, .down) }
            }
            Section {
                if !addon.version.isEmpty {
                    LabeledContent("Version", value: addon.version)
                }
                if !addon.capabilities.isEmpty {
                    LabeledContent("Provides", value: addon.capabilities.joined(separator: ", "))
                }
            } header: {
                Text("About")
            } footer: {
                if !addon.summary.isEmpty {
                    Text(addon.summary)
                }
            }
            Section {
                Button("Remove Addon") { isConfirmingRemoval = true }
            }
        }
    }

    private func enabled(_ addon: TvosAddon) -> Binding<Bool> {
        Binding(
            get: { addon.enabled },
            set: { isOn in if isOn != addon.enabled { model.toggle(addon) } }
        )
    }

    private func remove(_ addon: TvosAddon) {
        model.remove(addon)
        dismiss()
    }
}
