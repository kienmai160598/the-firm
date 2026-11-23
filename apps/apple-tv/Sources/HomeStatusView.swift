import SwiftUI

struct HomeStatusView: View {
    let errorMessage: String?

    var body: some View {
        Text(errorMessage ?? "No catalogs yet")
            .foregroundStyle(.secondary)
    }
}
