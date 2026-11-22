import SwiftUI

struct HomeStatusView: View {
    let isLoading: Bool
    let errorMessage: String?

    var body: some View {
        VStack(spacing: 24) {
            if isLoading {
                ProgressView()
                Text("Loading catalogs")
            } else {
                Text(errorMessage ?? "No catalogs yet")
                    .foregroundStyle(.secondary)
            }
        }
    }
}
