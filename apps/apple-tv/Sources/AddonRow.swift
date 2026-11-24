import SwiftUI
import NuvioTvShared

/// One row of the installed list: the name, its state where Settings shows a value, and the
/// chevron of a row that opens a page. The row is a `RouteLink`, not a `NavigationLink`: inside the
/// sidebar `TabView` a `NavigationLink` in a `List` does not push onto the app's `NavigationStack`.
struct AddonRow: View {
    let addon: TvosAddon

    var body: some View {
        LabeledContent(addon.name) {
            HStack(spacing: 24) {
                state
                Image(systemName: "chevron.forward")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
                    .accessibilityHidden(true)
            }
        }
    }

    @ViewBuilder
    private var state: some View {
        if addon.isRefreshing {
            ProgressView()
        } else if addon.errorMessage != nil {
            Text("Error")
                .foregroundStyle(.red)
        } else {
            Text(addon.enabled ? "On" : "Off")
        }
    }
}
