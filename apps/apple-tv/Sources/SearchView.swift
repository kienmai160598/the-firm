import NuvioTvShared
import SwiftUI

struct SearchView: View {
    @StateObject private var model = SearchModel()
    @State private var query = ""

    var body: some View {
        NavigationStack {
            searchableResults
        }
    }

    private var searchableResults: some View {
        results
            .searchable(text: $query, prompt: "Movies and shows")
            .onChange(of: query) { _, newQuery in model.queryChanged(newQuery) }
            .onAppear(perform: model.observe)
            .onDisappear(perform: model.stop)
    }

    @ViewBuilder
    private var results: some View {
        if model.rows.isEmpty {
            SearchStatusView(isLoading: model.isLoading, message: statusMessage)
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

    private var statusMessage: String? {
        query.trimmingCharacters(in: .whitespaces).isEmpty ? nil : model.message
    }
}

private struct SearchStatusView: View {
    let isLoading: Bool
    let message: String?

    var body: some View {
        Group {
            if isLoading {
                ProgressView()
            } else if let message {
                Text(message)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, minHeight: 300)
    }
}
