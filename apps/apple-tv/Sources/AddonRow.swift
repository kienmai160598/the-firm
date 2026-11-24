import SwiftUI
import NuvioTvShared

/// One row of the installed list: the name and its state where Settings shows a value.
struct AddonRow: View {
    let addon: TvosAddon

    var body: some View {
        SettingsLinkLabel(title: addon.name) {
            state
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
