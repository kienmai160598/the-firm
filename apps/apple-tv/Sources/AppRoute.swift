import NuvioTvShared

enum AppRoute: Hashable {
    case details(type: String, id: String)
    case streams(TvosStreamTarget, resumesDirectly: Bool = false)
}
