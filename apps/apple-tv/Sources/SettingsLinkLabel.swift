import SwiftUI

/// A Settings row that opens a page: the title, its value where Settings shows one, and the
/// chevron. Rows are `RouteLink`s, not `NavigationLink`s: inside the sidebar `TabView` a
/// `NavigationLink` in a `List` does not push onto the app's `NavigationStack`.
struct SettingsLinkLabel<Value: View>: View {
    let title: String
    @ViewBuilder let value: Value

    var body: some View {
        LabeledContent(title) {
            HStack(spacing: 24) {
                value
                Image(systemName: "chevron.forward")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
                    .accessibilityHidden(true)
            }
        }
    }
}

extension SettingsLinkLabel where Value == EmptyView {
    init(title: String) {
        self.init(title: title) { EmptyView() }
    }
}
