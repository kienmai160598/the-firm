import NuvioTvShared

enum AppRoute: Hashable {
    case details(type: String, id: String)
    case streams(TvosStreamTarget, resumesDirectly: Bool = false)
    case addon(manifestUrl: String)
    case settings(SettingsPage)
}
