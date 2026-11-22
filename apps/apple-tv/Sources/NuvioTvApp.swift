import SwiftUI
import NuvioTvShared

@main
struct NuvioTvApp: App {
    init() {
        #if DEBUG
        DebugLaunch.eraseStateWhenAsked()
        #endif
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
