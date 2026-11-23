import SwiftUI

struct RootTabView: View {
    @StateObject private var router = Router()
    @StateObject private var curtain = LaunchCurtain()

    var body: some View {
        tabs
            .environmentObject(router)
            .environmentObject(curtain)
            .overlay { LaunchCurtainCover(curtain: curtain) }
            .onAppear(perform: openDebugRoute)
            .onAppear(perform: curtain.liftIfContentNeverTakesFocus)
            .onChange(of: router.path.isEmpty) { _, isHome in
                if !isHome { curtain.liftNow() }
            }
    }

    @ViewBuilder
    private var tabs: some View {
        NavigationStack(path: $router.path) {
            rootTabs.withAppDestinations()
        }
    }

    @ViewBuilder
    private var rootTabs: some View {
        if #available(tvOS 18, *) {
            SidebarTabs()
        } else {
            TopTabs()
        }
    }

    private func openDebugRoute() {
        #if DEBUG
        guard router.path.isEmpty, let route = DebugLaunch.initialRoute else { return }
        router.open(route)
        curtain.liftNow()
        #endif
    }
}

extension View {
    func withAppDestinations() -> some View {
        navigationDestination(for: AppRoute.self, destination: AppDestination.init)
    }
}

private struct AppDestination: View {
    let route: AppRoute

    var body: some View {
        switch route {
        case let .details(type, id):
            DetailsView(type: type, id: id)
        case let .streams(target, resumesDirectly):
            StreamsView(target: target, resumesDirectly: resumesDirectly)
        }
    }
}
