import NuvioTvShared
import SwiftUI

struct HomeView: View {
    @StateObject private var model = HomeModel()

    var body: some View {
        content
            .onAppear(perform: model.observe)
            .onDisappear(perform: model.stop)
    }

    @ViewBuilder
    private var content: some View {
        if model.rows.isEmpty && model.continueWatching == nil && model.upcoming == nil {
            HomeStatusView(isLoading: model.isLoading, errorMessage: model.errorMessage)
        } else {
            ScrollView(.vertical) {
                LazyVStack(alignment: .leading, spacing: 48) {
                    ForEach(continueWatchingRows, id: \.title) { row in
                        ContinueWatchingRowView(
                            row: row,
                            onRemove: model.removeFromContinueWatching
                        )
                    }
                    ForEach(model.rows, id: \.key) { row in
                        CatalogRowView(row: row)
                    }
                }
                .padding(.vertical, 40)
            }
        }
    }

    private var continueWatchingRows: [TvosContinueWatchingRow] {
        [model.continueWatching, model.upcoming].compactMap { $0 }
    }
}
