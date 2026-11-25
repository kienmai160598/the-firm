import NuvioTvShared
import SwiftUI

struct SearchView: View {
    @StateObject private var model = SearchModel()
    @EnvironmentObject private var router: Router
    @State private var query = ""

    var body: some View {
        NavigationStack {
            searchableContent
        }
    }

    private var searchableContent: some View {
        content
            .searchable(text: $query, prompt: "Movies and shows")
            .onChange(of: query) { _, newQuery in model.queryChanged(newQuery) }
            .onChange(of: router.path.count) { previous, current in
                if current > previous { model.resultOpened(for: query) }
            }
            .onAppear(perform: model.observe)
            .onDisappear(perform: model.stop)
    }

    @ViewBuilder
    private var content: some View {
        if isQueryEmpty {
            SearchStartView(
                recentSearches: model.recentSearches,
                discover: model.discover,
                onPick: { query = $0 },
                onRemove: model.removeRecentSearch
            )
        } else if model.rows.isEmpty {
            SearchStatusView(isLoading: model.isLoading, message: model.message)
        } else {
            SearchRows(rows: model.rows)
        }
    }

    private var isQueryEmpty: Bool {
        query.trimmingCharacters(in: .whitespaces).isEmpty
    }
}

private struct SearchRows: View {
    let rows: [TvosRow]

    var body: some View {
        ScrollView(.vertical) {
            LazyVStack(alignment: .leading, spacing: 24) {
                ForEach(rows, id: \.key) { row in
                    CatalogRowView(row: row)
                }
            }
            .padding(.bottom, 60)
        }
    }
}

private struct SearchStartView: View {
    let recentSearches: [String]
    let discover: TvosRow?
    let onPick: (String) -> Void
    let onRemove: (String) -> Void

    var body: some View {
        ScrollView(.vertical) {
            VStack(alignment: .leading, spacing: 24) {
                if !recentSearches.isEmpty {
                    RecentSearchesRow(queries: recentSearches, onPick: onPick, onRemove: onRemove)
                }
                if let discover {
                    CatalogRowView(row: discover)
                }
            }
            .padding(.bottom, 60)
        }
    }
}

private struct RecentSearchesRow: View {
    let queries: [String]
    let onPick: (String) -> Void
    let onRemove: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ShelfHeader(title: "Recent Searches")
            ScrollView(.horizontal) {
                LazyHStack(spacing: 24) {
                    ForEach(queries, id: \.self) { recentQuery in
                        Button(recentQuery) { onPick(recentQuery) }
                            .contextMenu {
                                Button("Remove", role: .destructive) { onRemove(recentQuery) }
                            }
                    }
                }
                .padding(.vertical, 30)
            }
            .scrollClipDisabled()
            .padding(.leading, -SystemButtonInset.horizontal)
        }
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
