import NuvioTvShared
import SwiftUI

struct HomeView: View {
    private static let heroId = "home-hero"

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
            ScrollViewReader { proxy in
                ScrollView(.vertical) {
                    shelves(onHeroFocus: { revealHero(using: proxy) })
                }
                .background(HomeBackground())
                .ignoresSafeArea(edges: .top)
            }
        }
    }

    private func shelves(onHeroFocus: @escaping () -> Void) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HomeHero(rows: model.rows, onFocus: onHeroFocus)
                .id(Self.heroId)
            VStack(alignment: .leading, spacing: 24) {
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
            .padding(.bottom, 60)
        }
    }

    private func revealHero(using proxy: ScrollViewProxy) {
        withAnimation(.smooth(duration: 0.35)) { proxy.scrollTo(Self.heroId, anchor: .top) }
    }

    private var continueWatchingRows: [TvosContinueWatchingRow] {
        [model.continueWatching, model.upcoming].compactMap { $0 }
    }
}

private struct HomeBackground: View {
    var body: some View {
        LinearGradient(
            colors: [
                Color(red: 0.17, green: 0.19, blue: 0.22),
                Color(red: 0.11, green: 0.12, blue: 0.14),
            ],
            startPoint: .top,
            endPoint: .bottom
        )
        .ignoresSafeArea()
    }
}
