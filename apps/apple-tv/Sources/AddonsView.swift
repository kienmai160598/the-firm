import SwiftUI
import NuvioTvShared

struct AddonsView: View {
    @StateObject private var model = AddonsModel()
    @State private var chosenAddon: TvosAddon?
    @State private var addonToRemove: TvosAddon?
    @FocusState private var focusedAddonUrl: String?

    var body: some View {
        HStack(alignment: .top, spacing: 80) {
            AddonInstallPanel(model: model)
                .frame(width: 560)
                .focusSection()
            installedList
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                .focusSection()
        }
        .defaultFocus($focusedAddonUrl, model.addons.first?.manifestUrl, priority: .userInitiated)
        .padding(.top, 60)
        .onAppear(perform: model.observe)
        .onDisappear(perform: model.stop)
        .confirmationDialog(
            chosenAddon?.name ?? "",
            isPresented: isPresenting($chosenAddon),
            titleVisibility: .visible,
            presenting: chosenAddon,
            actions: addonActions,
            message: { addon in Text(addon.enabled ? "On" : "Off") }
        )
        .alert(
            "Remove \(addonToRemove?.name ?? "")?",
            isPresented: isPresenting($addonToRemove),
            presenting: addonToRemove,
            actions: removalActions,
            message: { _ in Text(Self.removalMessage) }
        )
    }

    private static let removalMessage = "Its catalogs, streams and subtitles leave the app."

    @ViewBuilder
    private var installedList: some View {
        if model.addons.isEmpty {
            ContentUnavailableView(
                "No addons",
                systemImage: "puzzlepiece.extension",
                description: Text("Add a manifest URL to get catalogs and streams.")
            )
        } else {
            List {
                Section {
                    ForEach(model.addons, id: \.manifestUrl) { addon in
                        Button { chosenAddon = addon } label: { AddonRow(addon: addon) }
                            .accessibilityIdentifier("addon-row")
                            .focused($focusedAddonUrl, equals: addon.manifestUrl)
                    }
                } header: {
                    Text("Installed")
                } footer: {
                    Text("Select an addon to turn it off, move it or remove it.")
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    @ViewBuilder
    private func addonActions(_ addon: TvosAddon) -> some View {
        Button(addon.enabled ? "Turn Off" : "Turn On") { model.toggle(addon) }
        if model.canMove(addon, .up) {
            Button("Move Up") { model.move(addon, .up) }
        }
        if model.canMove(addon, .down) {
            Button("Move Down") { model.move(addon, .down) }
        }
        Button("Remove", role: .destructive) { addonToRemove = addon }
        Button("Cancel", role: .cancel) {}
    }

    @ViewBuilder
    private func removalActions(_ addon: TvosAddon) -> some View {
        Button("Remove", role: .destructive) { model.remove(addon) }
        Button("Cancel", role: .cancel) {}
    }

    private func isPresenting(_ addon: Binding<TvosAddon?>) -> Binding<Bool> {
        Binding(
            get: { addon.wrappedValue != nil },
            set: { isShown in if !isShown { addon.wrappedValue = nil } }
        )
    }
}
