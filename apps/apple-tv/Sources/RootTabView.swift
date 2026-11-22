import SwiftUI

struct RootTabView: View {
    @StateObject private var router = Router()

    var body: some View {
        NavigationStack(path: $router.path) {
            tabs.navigationDestination(for: AppRoute.self, destination: destination)
        }
        .environmentObject(router)
        .onAppear(perform: openDebugRoute)
    }

    private var tabs: some View {
        TabView {
            HomeView()
                .tabItem { Text("Home") }
            PlaceholderView(title: "Search")
                .tabItem { Text("Search") }
            PlaceholderView(title: "Library")
                .tabItem { Text("Library") }
            PlaceholderView(title: "Settings")
                .tabItem { Text("Settings") }
        }
    }

    @ViewBuilder
    private func destination(for route: AppRoute) -> some View {
        switch route {
        case let .details(type, id):
            DetailsView(type: type, id: id)
        case let .streams(target, resumesDirectly):
            StreamsView(target: target, resumesDirectly: resumesDirectly)
        }
    }

    private func openDebugRoute() {
        #if DEBUG
        guard router.path.isEmpty, let route = DebugLaunch.initialRoute else { return }
        router.open(route)
        #endif
    }
}
