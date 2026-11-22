import SwiftUI
import NuvioTvShared

struct DetailsView: View {
    @StateObject private var model: DetailsModel
    @EnvironmentObject private var router: Router

    init(type: String, id: String) {
        _model = StateObject(wrappedValue: DetailsModel(type: type, id: id))
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            BackdropView(url: model.details?.backdropUrl)
            content
        }
        .onAppear(perform: model.observe)
        .onDisappear(perform: model.stop)
        #if DEBUG
        .onChange(of: model.details) { _, details in openDebugStreams(for: details) }
        #endif
    }

    @ViewBuilder
    private var content: some View {
        if let details = model.details {
            DetailsContentView(details: details)
        } else if model.isLoading {
            ProgressView()
        } else {
            DetailsErrorView(message: model.errorMessage)
        }
    }

    #if DEBUG
    private func openDebugStreams(for details: TvosMetaDetails?) {
        guard let details, let route = DebugLaunch.streamsRouteOnce(for: details) else { return }
        router.open(route)
    }
    #endif
}

private struct DetailsContentView: View {
    let details: TvosMetaDetails

    var body: some View {
        ScrollView(.vertical) {
            VStack(alignment: .leading, spacing: 40) {
                DetailsHeroView(details: details)
                    .frame(minHeight: heroMinHeight, alignment: .bottomLeading)
                if !details.seasons.isEmpty {
                    EpisodesSection(
                        seasons: details.seasons,
                        preferredSeasonNumber: preferredSeasonNumber
                    )
                }
                if !details.cast.isEmpty {
                    CastSection(cast: details.cast)
                }
            }
            .padding(.bottom, 40)
        }
    }

    private var heroMinHeight: CGFloat {
        details.seasons.isEmpty ? 860 : 480
    }

    private var preferredSeasonNumber: Int32? {
        details.primaryAction?.target.seasonNumber?.int32Value
    }
}

private struct DetailsErrorView: View {
    let message: String?

    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 80))
            Text(message ?? "Could not load details")
                .font(.headline)
        }
        .foregroundStyle(.secondary)
    }
}
