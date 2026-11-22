import Foundation
import NuvioTvShared

@MainActor
final class AddonsModel: ObservableObject {
    @Published private(set) var addons: [TvosAddon] = []
    @Published private(set) var isInstalling = false
    @Published private(set) var statusMessage: String?

    private var subscription: TvosCancellable?

    func observe() {
        guard subscription == nil else { return }
        subscription = TvosAddons.shared.observe { [weak self] addons in
            self?.addons = addons
        }
    }

    func stop() {
        subscription?.cancel()
        subscription = nil
    }

    func install(_ rawUrl: String) {
        let url = rawUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !url.isEmpty, !isInstalling else { return }
        isInstalling = true
        statusMessage = nil
        TvosAddons.shared.install(rawUrl: url) { [weak self] result in
            self?.finishInstall(result)
        }
    }

    func remove(_ addon: TvosAddon) {
        TvosAddons.shared.remove(manifestUrl: addon.manifestUrl)
    }

    func toggle(_ addon: TvosAddon) {
        TvosAddons.shared.setEnabled(manifestUrl: addon.manifestUrl, enabled: !addon.enabled)
    }

    private func finishInstall(_ result: TvosAddonInstallResult) {
        isInstalling = false
        switch result {
        case let installed as TvosAddonInstallResultInstalled:
            statusMessage = "Installed \(installed.name)"
        case let failed as TvosAddonInstallResultFailed:
            statusMessage = failed.message
        default:
            statusMessage = nil
        }
    }
}
