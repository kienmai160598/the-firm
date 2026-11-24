import SwiftUI
import NuvioTvShared

/// Laid out like the Settings app: a system `List` on the right, and on the left a preview of the
/// focused row (the addon's logo and summary, or the QR code while "Add Addon" has focus).
struct AddonsView: View {
    @StateObject private var model: AddonsModel
    @StateObject private var qrServer: AddonQrServer
    @State private var manifestUrl = ""
    @State private var previewed: AddonFocus = .add
    @FocusState private var focus: AddonFocus?

    init() {
        let model = AddonsModel()
        _model = StateObject(wrappedValue: model)
        _qrServer = StateObject(wrappedValue: AddonQrServer { url, onFinish in
            model.install(url, onFinish: onFinish)
        })
    }

    var body: some View {
        SettingsSplit {
            preview
        } list: {
            list
        }
        .defaultFocus($focus, .add)
        .onChange(of: focus) { _, focus in
            if let focus { previewed = focus }
        }
        .onChange(of: model.installStatus) { _, status in
            if case .installed = status { manifestUrl = "" }
        }
        .onAppear {
            model.observe()
            qrServer.start()
        }
        .onDisappear {
            model.stop()
            qrServer.stop()
        }
    }

    @ViewBuilder
    private var preview: some View {
        if case .addon(let url) = previewed, let addon = model.addon(url) {
            AddonSummary(addon: addon)
        } else {
            AddonInstallPanel(
                pageUrl: qrServer.pageUrl,
                status: model.installStatus,
                hasAddons: !model.addons.isEmpty
            )
        }
    }

    private var list: some View {
        List {
            TextField("Add Addon", text: $manifestUrl)
                .textContentType(.URL)
                .autocorrectionDisabled()
                .onSubmit { model.install(manifestUrl) }
                .focused($focus, equals: .add)
            if !model.addons.isEmpty {
                Section("Installed") {
                    ForEach(model.addons, id: \.manifestUrl) { addon in
                        RouteLink(value: .addon(manifestUrl: addon.manifestUrl)) {
                            AddonRow(addon: addon)
                        }
                        .accessibilityIdentifier("addon-row")
                        .focused($focus, equals: .addon(addon.manifestUrl))
                    }
                }
            }
        }
    }
}

private enum AddonFocus: Hashable {
    case add
    case addon(String)
}
