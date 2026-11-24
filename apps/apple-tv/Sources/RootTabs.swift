import SwiftUI

enum RootTab: Hashable, CaseIterable {
    case home
    case search
    case library
    case settings

    var title: String {
        switch self {
        case .home: "Home"
        case .search: "Search"
        case .library: "Library"
        case .settings: "Settings"
        }
    }

    var symbol: String {
        switch self {
        case .home: "house"
        case .search: "magnifyingglass"
        case .library: "books.vertical"
        case .settings: "gearshape"
        }
    }

    @ViewBuilder
    var screen: some View {
        switch self {
        case .home: HomeView()
        case .search: SearchView()
        case .settings: SettingsView()
        case .library: LibraryView()
        }
    }
}

@available(tvOS 18, *)
struct SidebarTabs: View {
    @State private var selection: RootTab = .home

    var body: some View {
        TabView(selection: $selection) {
            ForEach(RootTab.allCases, id: \.self) { tab in
                if tab == .search {
                    Tab(value: tab, role: .search) { tab.screen }
                } else {
                    Tab(tab.title, systemImage: tab.symbol, value: tab) { tab.screen }
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
