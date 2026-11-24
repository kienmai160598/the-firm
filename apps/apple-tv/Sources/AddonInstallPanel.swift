import SwiftUI

struct AddonInstallPanel: View {
    @ObservedObject var model: AddonsModel
    @StateObject private var qrServer: AddonQrServer
    @State private var manifestUrl = ""

    init(model: AddonsModel) {
        self.model = model
        _qrServer = StateObject(wrappedValue: AddonQrServer { url, onFinish in
            model.install(url, onFinish: onFinish)
        })
    }

    private static let explanation =
        "Addons bring the catalogs, streams and subtitles you see in the app."

    var body: some View {
        VStack(alignment: .leading, spacing: 28) {
            Text("Addons")
                .font(.title2.bold())
            Text(Self.explanation)
                .font(.callout)
                .foregroundStyle(.secondary)
            AddonQrCode(pageUrl: qrServer.pageUrl)
            TextField("Or type a manifest URL", text: $manifestUrl)
                .textContentType(.URL)
                .autocorrectionDisabled()
                .onSubmit(install)
            Button(action: install) { installLabel }
                .disabled(model.isInstalling || manifestUrl.isEmpty)
            statusLine
        }
        .onChange(of: model.installStatus) { _, status in
            if case .installed = status { manifestUrl = "" }
        }
        .onAppear(perform: qrServer.start)
        .onDisappear(perform: qrServer.stop)
    }

    @ViewBuilder
    private var installLabel: some View {
        if model.isInstalling {
            HStack(spacing: 16) {
                ProgressView()
                Text("Installing")
            }
        } else {
            Label("Install", systemImage: "plus")
        }
    }

    @ViewBuilder
    private var statusLine: some View {
        switch model.installStatus {
        case .installed(let name):
            Label("Installed \(name)", systemImage: "checkmark.circle.fill")
                .font(.callout)
                .foregroundStyle(.secondary)
        case .failed(let message):
            VStack(alignment: .leading, spacing: 8) {
                Label("Couldn't add this addon", systemImage: "exclamationmark.triangle.fill")
                    .font(.callout)
                    .foregroundStyle(.red)
                Text(message)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
        case .installing, nil:
            EmptyView()
        }
    }

    private func install() {
        model.install(manifestUrl)
    }
}

/// Scan with a phone camera to open the add-addon page the TV serves on the local network.
private struct AddonQrCode: View {
    private static let side: CGFloat = 220

    let pageUrl: URL?

    var body: some View {
        HStack(alignment: .center, spacing: 28) {
            code
                .frame(width: Self.side, height: Self.side)
            VStack(alignment: .leading, spacing: 10) {
                Text("Add from your phone")
                    .font(.headline)
                Text(caption)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .fixedSize(horizontal: false, vertical: true)
        }
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("addonQrCode")
        .accessibilityValue(pageUrl?.absoluteString ?? "")
    }

    @ViewBuilder
    private var code: some View {
        if let pageUrl, let image = QrCodeImage.make(pageUrl.absoluteString) {
            Image(decorative: image, scale: 1)
                .interpolation(.none)
                .resizable()
                .padding(14)
                .background(.white, in: RoundedRectangle(cornerRadius: 16))
        } else {
            RoundedRectangle(cornerRadius: 16)
                .fill(.white.opacity(0.08))
                .overlay { Image(systemName: "qrcode").font(.system(size: 64)).foregroundStyle(.secondary) }
        }
    }

    private var caption: String {
        guard pageUrl != nil else { return "Connect the TV to your home network to add from a phone." }
        return "Scan with the camera, then paste the manifest URL on the page that opens."
    }
}
