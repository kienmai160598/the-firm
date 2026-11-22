import SwiftUI

@MainActor
final class Router: ObservableObject {
    @Published var path: [AppRoute] = []

    func open(_ route: AppRoute) {
        path.append(route)
    }
}
