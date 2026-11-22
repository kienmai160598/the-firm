import SwiftUI

struct RootTabView: View {
    @StateObject private var router = Router()

    var body: some View {
        tabs
            .environmentObject(router)
            .onAppear(perform: openDebugRoute)
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
