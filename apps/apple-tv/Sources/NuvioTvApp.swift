import SwiftUI
import NuvioTvShared

@main
struct NuvioTvApp: App {
    init() {
        TvosHome.shared.start()
        #if DEBUG
        TvosDebugAddon.shared.sync(manifestUrl: DebugLaunch.addonUrl)
        #endif
    }

    var body: some Scene {
        WindowGroup {
            RootTabView()
        }
    }
}
