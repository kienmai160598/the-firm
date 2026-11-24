import SwiftUI
import NuvioTvShared

struct DetailsView: View {
    @StateObject private var model: DetailsModel
    @StateObject private var trailer = HeroTrailer()
    @EnvironmentObject private var router: Router
    @FocusState private var heroFocus: DetailsHeroFocus?

    init(type: String, id: String) {
        _model = StateObject(wrappedValue: DetailsModel(type: type, id: id))
    }

    var body: some View {
        ZStack {
            DetailsBackground()
            BackdropView(
                url: model.details?.backdropUrl,
                isReceded: !isHeroFocused,
                trailer: trailer.picture(for: model.id)
            )
            content
        }
        .animation(.smooth(duration: 0.4), value: isHeroFocused)
        .onAppear {
            model.observe()
            updateTrailer()
        }
        .onDisappear {
            model.stop()
            trailer.stop()
        }
        .onChange(of: isTrailerDue) { updateTrailer() }
        #if DEBUG
        .onChange(of: model.details) { _, details in openDebugStreams(for: details) }
        #endif
    }

    /// The trailer plays behind the title while a hero button (Play or Library) has focus, and
    /// fades out once the viewer scrolls down to the episodes, the cast or the info.
    private var isTrailerDue: Bool {
        model.details != nil && isHeroFocused
    }

    private var isHeroFocused: Bool {
        heroFocus != nil
    }

    private func updateTrailer() {
        isTrailerDue ? trailer.play(type: model.type, id: model.id) : trailer.stop()
    }

    @ViewBuilder
    private var content: some View {
        if let details = model.details {
            DetailsContentView(details: details, heroFocus: $heroFocus)
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
    private static let heroScreenFraction: CGFloat = 0.88

    let details: TvosMetaDetails
    let heroFocus: FocusState<DetailsHeroFocus?>.Binding

    var body: some View {
        ScrollView(.vertical) {
            VStack(alignment: .leading, spacing: 56) {
                DetailsHeroView(details: details, focus: heroFocus)
                    .containerRelativeFrame(.vertical, alignment: .bottomLeading) { height, _ in
                        height * Self.heroScreenFraction
                    }
                if !details.seasons.isEmpty {
                    EpisodesSection(
                        metaId: details.metaId,
                        seasons: details.seasons,
                        preferredSeasonNumber: preferredSeasonNumber
                    )
                }
                if !details.cast.isEmpty {
                    CastSection(cast: details.cast)
                }
                DetailsInfoSection(details: details)
            }
            .padding(.bottom, 80)
        }
    }

    private var preferredSeasonNumber: Int32? {
        details.primaryAction?.target.seasonNumber?.int32Value
    }
}

private struct DetailsBackground: View {
    var body: some View {
        LinearGradient(
            colors: [
                Color(red: 0.13, green: 0.14, blue: 0.17),
                Color(red: 0.08, green: 0.09, blue: 0.1),
            ],
            startPoint: .top,
            endPoint: .bottom
        )
        .ignoresSafeArea()
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
