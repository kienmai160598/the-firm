#if DEBUG
import Foundation
import NuvioTvShared

enum DebugLaunch {
    private static let defaults = UserDefaults.standard
    private static var hasOpenedStreams = false
    private static var hasOpenedFirstStream = false

    static var initialRoute: AppRoute? {
        guard let route = defaults.string(forKey: "MoviesDebugRoute") else { return nil }
        let parts = route.split(separator: "/", maxSplits: 2).map(String.init)
        guard parts.count == 3, parts[0] == "details" || parts[0] == "streams" else { return nil }
        return .details(type: parts[1], id: parts[2])
    }

    static var usesStreamFixture: Bool {
        defaults.bool(forKey: "MoviesDebugStreamFixture")
    }

    static var opensFirstStream: Bool {
        !hasOpenedFirstStream && defaults.bool(forKey: "MoviesDebugOpenFirstStream")
    }

    static func markFirstStreamOpened() {
        hasOpenedFirstStream = true
    }

    static var addonUrl: String? {
        defaults.string(forKey: "MoviesDebugAddonUrl")
    }

    static var startSecondsBeforeEnd: Int64? {
        let seconds = defaults.integer(forKey: "MoviesDebugStartSecondsBeforeEnd")
        return seconds > 0 ? Int64(seconds) : nil
    }

    static var resumeChoice: Bool? {
        switch defaults.string(forKey: "MoviesDebugResumeChoice") {
        case "resume": return true
        case "startOver": return false
        default: return nil
        }
    }

    static var seasonNumber: Int32? {
        let season = defaults.integer(forKey: "MoviesDebugSeason")
        return season > 0 ? Int32(season) : nil
    }

    static func streamsRouteOnce(for details: TvosMetaDetails) -> AppRoute? {
        guard !hasOpenedStreams,
              defaults.string(forKey: "MoviesDebugRoute")?.hasPrefix("streams/") == true,
              let action = details.primaryAction
        else { return nil }
        hasOpenedStreams = true
        return .streams(action.target)
    }
}
#endif
