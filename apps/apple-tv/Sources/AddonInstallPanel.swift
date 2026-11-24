import SwiftUI

/// The preview while "Add Addon" has focus: the QR code a phone scans to send a manifest URL,
/// and how the last install went.
struct AddonInstallPanel: View {
    let pageUrl: URL?
    let status: AddonInstallStatus?
    let hasAddons: Bool

    var body: some View {
        SettingsPreview {
            code
                .accessibilityElement()
                .accessibilityIdentifier("addonQrCode")
                .accessibilityValue(pageUrl?.absoluteString ?? "")
        } caption: {
            if !hasAddons {
                Text("No addons")
                    .font(.headline)
            }
            Text(caption)
                .foregroundStyle(.secondary)
            statusLine
        }
    }

    @ViewBuilder
    private var code: some View {
        if let pageUrl, let image = QrCodeImage.make(pageUrl.absoluteString) {
            Image(decorative: image, scale: 1)
                .interpolation(.none)
                .resizable()
                .padding(56)
                .background(.white, in: RoundedRectangle(cornerRadius: SettingsMetrics.artRadius, style: .continuous))
        } else {
            RoundedRectangle(cornerRadius: SettingsMetrics.artRadius, style: .continuous)
                .fill(.quaternary)
                .overlay {
                    Image(systemName: "qrcode")
                        .font(.system(size: 200))
                        .foregroundStyle(.secondary)
                }
        }
    }

    private var caption: String {
        guard pageUrl != nil else {
            return "Connect the TV to your home network to add from a phone, or select Add Addon to type a manifest URL."
        }
        return "Scan with your phone's camera, then paste the manifest URL on the page that opens. "
            + "Or select Add Addon to type it."
    }

    @ViewBuilder
    private var statusLine: some View {
        switch status {
        case .installing:
            HStack(spacing: 16) {
                ProgressView()
                Text("Installing")
            }
        case .installed(let name):
            Label("Installed \(name)", systemImage: "checkmark.circle.fill")
                .foregroundStyle(.secondary)
        case .failed(let message):
            Label("Couldn't add this addon", systemImage: "exclamationmark.triangle.fill")
                .foregroundStyle(.red)
            Text(message)
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(2)
        case nil:
            EmptyView()
        }
    }
}
