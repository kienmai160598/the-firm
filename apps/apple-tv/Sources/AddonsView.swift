import SwiftUI
import NuvioTvShared

struct AddonsView: View {
    @StateObject private var model = AddonsModel()
    @State private var manifestUrl = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 40) {
                installSection
                addonList
            }
            .padding(.horizontal, 80)
            .padding(.vertical, 40)
        }
        .onAppear(perform: model.observe)
        .onDisappear(perform: model.stop)
    }

    private var installSection: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Add an addon")
                .font(.title3.bold())
            HStack(spacing: 24) {
                TextField("Manifest URL (https://.../manifest.json)", text: $manifestUrl)
                    .textContentType(.URL)
                    .autocorrectionDisabled()
                    .onSubmit(install)
                Button(installTitle, systemImage: "plus", action: install)
                    .disabled(model.isInstalling || manifestUrl.isEmpty)
            }
            if let statusMessage = model.statusMessage {
                Text(statusMessage)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var addonList: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Installed")
                .font(.title3.bold())
            ForEach(model.addons, id: \.manifestUrl) { addon in
                AddonRow(
                    addon: addon,
                    onToggle: { model.toggle(addon) },
                    onRemove: { model.remove(addon) }
                )
            }
        }
    }

    private var installTitle: String {
        model.isInstalling ? "Installing" : "Install"
    }

    private func install() {
        model.install(manifestUrl)
        manifestUrl = ""
    }
}

private struct AddonRow: View {
    let addon: TvosAddon
    let onToggle: () -> Void
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 24) {
            VStack(alignment: .leading, spacing: 8) {
                Text(addon.name)
                    .font(.headline)
                Text(summary)
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Button(
                addon.enabled ? "On" : "Off",
                systemImage: addon.enabled ? "checkmark.circle" : "circle",
                action: onToggle
            )
            Button("Remove", systemImage: "trash", role: .destructive, action: onRemove)
        }
    }

    private var summary: String {
        if let error = addon.errorMessage { return error }
        let streams = addon.providesStreams ? "Streams" : "No streams"
        return addon.description.isEmpty ? streams : "\(streams) · \(addon.description)"
    }
}
