import SwiftUI

enum RootTab: Hashable, CaseIterable {
    case home
    case search
    case library
    case addons
    case settings

    var title: String {
        switch self {
        case .home: "Home"
        case .search: "Search"
        case .library: "Library"
        case .addons: "Addons"
        case .settings: "Settings"
        }
    }

    var symbol: String {
        switch self {
        case .home: "house"
        case .search: "magnifyingglass"
        case .library: "books.vertical"
        case .addons: "puzzlepiece.extension"
        case .settings: "gearshape"
        }
    }

    @ViewBuilder
    var screen: some View {
        switch self {
        case .home: HomeView()
        case .addons: AddonsView()
        case .search, .library, .settings: PlaceholderView(title: title)
        }
    }
}

@available(tvOS 18, *)
struct SidebarTabs: View {
    @State private var selection: RootTab = .home

    var body: some View {
        TabView(selection: $selection) {
            ForEach(RootTab.allCases, id: \.self) { tab in
                Tab(tab.title, systemImage: tab.symbol, value: tab) {
                    tab.screen
                }
            }
        }
        .tabViewStyle(.sidebarAdaptable)
    }
}

struct TopTabs: View {
    var body: some View {
        TabView {
            ForEach(RootTab.allCases, id: \.self) { tab in
                tab.screen
                    .tabItem { Text(tab.title) }
            }
        }
    }
}
