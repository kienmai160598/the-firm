#if DEBUG
import Foundation
import NuvioTvShared

enum DebugLaunch {
    private static let defaults = UserDefaults.standard
    private static var hasOpenedStreams = false
    private static var hasOpenedFirstStream = false

    private static let payloadDirectory = "Payloads"

    static func eraseStateWhenAsked() {
        guard defaults.bool(forKey: "MoviesDebugFreshState"),
              let bundleId = Bundle.main.bundleIdentifier
        else { return }
        defaults.removePersistentDomain(forName: bundleId)
        let fileManager = FileManager.default
        let caches = fileManager.urls(for: .cachesDirectory, in: .userDomainMask)[0]
        let payloads = caches.appendingPathComponent(payloadDirectory)
        guard fileManager.fileExists(atPath: payloads.path) else { return }
        do {
            try fileManager.removeItem(at: payloads)
        } catch {
            assertionFailure("Could not erase \(payloads.path): \(error)")
        }
    }

    static var initialRoute: AppRoute? {
        guard let route = defaults.string(forKey: "MoviesDebugRoute") else { return nil }
        let parts = route.split(separator: "/", maxSplits: 2).map(String.init)
        guard parts.count == 3, parts[0] == "details" || parts[0] == "streams" else { return nil }
        return .details(type: parts[1], id: parts[2])
    }

    static var usesStreamFixture: Bool {
        defaults.bool(forKey: "MoviesDebugStreamFixture")
    }

    /// Keeps the Home hero on its loading placeholder, so a test can see where it sits.
    static var holdsHomeHeroPlaceholder: Bool {
        defaults.bool(forKey: "MoviesDebugHoldHomeHeroPlaceholder")
    }

    static var streamFixtureFillerRows: Int {
        defaults.integer(forKey: "MoviesDebugStreamFixtureFillerRows")
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
