import NuvioTvShared
import SwiftUI

/// Saved titles, one shelf per section that `LibraryRepository` gives: by type for the local list,
/// by list (Watchlist and the others) when a tracker is the library source.
struct LibraryView: View {
    @StateObject private var model = LibraryModel()

    var body: some View {
        content
            .onAppear(perform: model.observe)
            .onDisappear(perform: model.stop)
    }

    @ViewBuilder
    private var content: some View {
        if model.rows.isEmpty {
            LibraryStatusView(isLoading: model.isLoading, errorMessage: model.errorMessage)
        } else {
            ScrollView(.vertical) {
                LazyVStack(alignment: .leading, spacing: 24) {
                    ForEach(model.rows, id: \.key) { row in
                        CatalogRowView(row: row)
                    }
                }
                .padding(.bottom, 60)
            }
        }
    }
}

/// Holds focus while there is nothing to show, so the sidebar does not stay open over an empty tab.
private struct LibraryStatusView: View {
    let isLoading: Bool
    let errorMessage: String?

    var body: some View {
        VStack(spacing: 16) {
            if isLoading {
                ProgressView()
            } else {
                Text(errorMessage ?? "Your Library is empty")
                    .font(.title2)
                Text("Add movies and shows from their page with the Library button.")
                    .foregroundStyle(.secondary)
            }
        }
        .padding(40)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .focusable()
        .accessibilityIdentifier("library-status")
    }
}
