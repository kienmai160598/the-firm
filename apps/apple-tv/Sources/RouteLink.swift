import SwiftUI

struct RouteLink<Label: View>: View {
    private let route: AppRoute
    private let label: Label
    @EnvironmentObject private var router: Router

    init(value route: AppRoute, @ViewBuilder label: () -> Label) {
        self.route = route
        self.label = label()
    }

    var body: some View {
        Button { router.open(route) } label: { label }
    }
}
