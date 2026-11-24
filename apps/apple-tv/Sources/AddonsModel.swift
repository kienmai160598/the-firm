import Foundation
import NuvioTvShared

enum AddonInstallStatus: Equatable {
    case installing
    case installed(name: String)
    case failed(message: String)
}

enum AddonMove {
    case up
    case down

    var offset: Int {
        self == .up ? -1 : 1
    }
}

@MainActor
final class AddonsModel: ObservableObject {
    @Published private(set) var addons: [TvosAddon] = TvosAddons.shared.current()
    @Published private(set) var installStatus: AddonInstallStatus?

    private var subscription: TvosCancellable?

    var isInstalling: Bool {
        installStatus == .installing
    }

    func addon(_ manifestUrl: String) -> TvosAddon? {
        addons.first { $0.manifestUrl == manifestUrl }
    }

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

    /// `onFinish` gets the outcome; `.installing` means another install was still running.
    func install(_ rawUrl: String, onFinish: ((AddonInstallStatus) -> Void)? = nil) {
        let url = rawUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !isInstalling else {
            onFinish?(.installing)
            return
        }
        guard !url.isEmpty else {
            onFinish?(.failed(message: "Enter a manifest URL."))
            return
        }
        installStatus = .installing
        TvosAddons.shared.install(rawUrl: url) { [weak self] result in
            self?.finishInstall(result)
            onFinish?(self?.installStatus ?? .failed(message: "The Addons screen closed."))
        }
    }

    func remove(_ addon: TvosAddon) {
        TvosAddons.shared.remove(manifestUrl: addon.manifestUrl)
    }

    func toggle(_ addon: TvosAddon) {
        TvosAddons.shared.setEnabled(manifestUrl: addon.manifestUrl, enabled: !addon.enabled)
    }

    func canMove(_ addon: TvosAddon, _ move: AddonMove) -> Bool {
        destination(of: addon, move) != nil
    }

    func move(_ addon: TvosAddon, _ move: AddonMove) {
        guard let from = index(of: addon), let to = destination(of: addon, move) else { return }
        TvosAddons.shared.move(fromIndex: Int32(from), toIndex: Int32(to))
    }

    private func index(of addon: TvosAddon) -> Int? {
        addons.firstIndex { $0.manifestUrl == addon.manifestUrl }
    }

    private func destination(of addon: TvosAddon, _ move: AddonMove) -> Int? {
        guard let from = index(of: addon) else { return nil }
        let to = from + move.offset
        return addons.indices.contains(to) ? to : nil
    }

    private func finishInstall(_ result: TvosAddonInstallResult) {
        switch result {
        case let installed as TvosAddonInstallResultInstalled:
            installStatus = .installed(name: installed.name)
        case let failed as TvosAddonInstallResultFailed:
            installStatus = .failed(message: failed.message)
        default:
            installStatus = nil
        }
    }
}
