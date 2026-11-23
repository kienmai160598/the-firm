import SwiftUI

struct AddonInstallPanel: View {
    @ObservedObject var model: AddonsModel
    @State private var manifestUrl = ""

    private static let explanation =
        "Addons bring the catalogs, streams and subtitles you see in the app."

    var body: some View {
        VStack(alignment: .leading, spacing: 28) {
            Image(systemName: "puzzlepiece.extension.fill")
                .font(.system(size: 96))
                .foregroundStyle(.secondary)
            Text("Addons")
                .font(.title2.bold())
            Text(Self.explanation)
                .font(.callout)
                .foregroundStyle(.secondary)
            Text("Add an addon")
                .font(.headline)
                .padding(.top, 20)
            TextField("Manifest URL", text: $manifestUrl)
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
