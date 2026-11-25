import SwiftUI
import NuvioTvShared

struct EpisodesSection: View {
    let metaId: String
    let seasons: [TvosSeason]
    var preferredSeasonNumber: Int32?

    @State private var selectedSeasonNumber: Int32?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            SeasonPicker(
                metaId: metaId,
                seasons: seasons,
                selectedNumber: selectedSeason?.number
            ) { season in
                selectedSeasonNumber = season.number
            }
            if let season = selectedSeason {
                EpisodeShelf(metaId: metaId, episodes: season.episodes)
                    .id(season.number)
            }
        }
        .onAppear(perform: selectInitialSeason)
    }

    private var selectedSeason: TvosSeason? {
        let wanted = selectedSeasonNumber ?? preferredSeasonNumber
        return seasons.first { $0.number == wanted } ?? seasons.first
    }

    private func selectInitialSeason() {
        #if DEBUG
        if selectedSeasonNumber == nil, let debugSeason = DebugLaunch.seasonNumber {
            selectedSeasonNumber = debugSeason
        }
        #endif
    }
}

private struct SeasonPicker: View {
    let metaId: String
    let seasons: [TvosSeason]
    let selectedNumber: Int32?
    let onSelect: (TvosSeason) -> Void
    @FocusState private var focusedNumber: Int32?

    var body: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 24) {
                ForEach(seasons, id: \.number) { season in
                    SeasonButton(
                        metaId: metaId,
                        season: season,
                        isSelected: season.number == selectedNumber
                    ) {
                        onSelect(season)
                    }
                    .focused($focusedNumber, equals: season.number)
                }
            }
            .padding(.vertical, 12)
        }
        .scrollClipDisabled()
        .padding(.leading, -SystemButtonInset.horizontal)
        .focusSection()
        .onChange(of: focusedNumber, focusSelectedSeasonOnEntry)
    }

    private func focusSelectedSeasonOnEntry(from previous: Int32?, to current: Int32?) {
        guard previous == nil, let current, let selectedNumber, current != selectedNumber else {
            return
        }
        focusedNumber = selectedNumber
    }
}

private struct SeasonButton: View {
    let metaId: String
    let season: TvosSeason
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                if isSelected {
                    Image(systemName: "checkmark")
                }
                Text(season.title)
            }
        }
        .glassButtonStyle()
        .contextMenu {
            if hasReleasedEpisodes {
                WatchedToggleButton(scope: "Season", isWatched: season.isWatched) {
                    // Selecting it shows the marks change, and focus returning from the menu
                    // lands on the selected season, so the picker does not snap away.
                    action()
                    TvosWatched.shared.toggleSeason(
                        id: metaId,
                        season: season.number,
                        areWatched: season.isWatched
                    )
                }
            }
        }
        .accessibilityIdentifier("season-\(season.number)")
    }

    private var hasReleasedEpisodes: Bool {
        season.episodes.contains(where: \.isReleased)
    }
}

/// "Mark as Watched" or "Mark as Unwatched" in a context menu; [scope] names what it marks.
private struct WatchedToggleButton: View {
    var scope: String?
    let isWatched: Bool
    let action: () -> Void

    var body: some View {
        Button(title, systemImage: isWatched ? "eye.slash" : "checkmark.circle", action: action)
    }

    private var title: String {
        let state = isWatched ? "Unwatched" : "Watched"
        guard let scope else { return "Mark as \(state)" }
        return "Mark \(scope) as \(state)"
    }
}

private struct EpisodeShelf: View {
    let metaId: String
    let episodes: [TvosEpisode]

    var body: some View {
        ScrollView(.horizontal) {
            LazyHStack(alignment: .top, spacing: 40) {
                ForEach(episodes, id: \.videoId) { episode in
                    EpisodeCard(metaId: metaId, episode: episode)
                }
            }
            .padding(.vertical, 24)
        }
        .scrollClipDisabled()
        .focusSection()
    }
}

private struct EpisodeCard: View {
    static let width: CGFloat = 400

    let metaId: String
    let episode: TvosEpisode

    var body: some View {
        RouteLink(value: AppRoute.streams(episode.streamTarget)) {
            thumbnail
            captions
        }
        .buttonStyle(.borderless)
        .cardButtonShape()
        .contextMenu {
            if episode.isReleased {
                WatchedToggleButton(isWatched: episode.isWatched) {
                    TvosWatched.shared.toggleEpisode(
                        id: metaId,
                        videoId: episode.videoId,
                        isWatched: episode.isWatched
                    )
                }
            }
        }
        .accessibilityIdentifier("episode-\(episode.videoId)")
        .accessibilityLabel(heading)
        .accessibilityValue(watchState)
    }

    private var thumbnail: some View {
        ArtworkImage(url: episode.thumbnailUrl)
            .frame(width: Self.width, height: Self.width * 9 / 16)
            .opacity(episode.isReleased ? 1 : 0.5)
            .overlay(alignment: .topTrailing) { watchedMark }
            .overlay(alignment: .bottom) { progressBar }
            .cardArtworkShape()
    }

    private var captions: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(heading)
                .font(.system(size: 25, weight: .semibold))
                .lineLimit(1)
            if let overview = episode.overview {
                Text(overview)
                    .font(.system(size: 21))
                    .foregroundStyle(.white.opacity(0.6))
                    .lineLimit(3)
            }
            if !details.isEmpty {
                FactLine(facts: details)
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(.white.opacity(0.45))
                    .lineLimit(1)
            }
        }
        .frame(width: Self.width, alignment: .leading)
    }

    @ViewBuilder
    private var watchedMark: some View {
        if episode.isWatched {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 44))
                .symbolRenderingMode(.palette)
                .foregroundStyle(.black, .white)
                .padding(CardShape.contentInset)
        }
    }

    @ViewBuilder
    private var progressBar: some View {
        if !episode.isWatched && episode.progressFraction > 0 {
            WatchProgressBar(fraction: Double(episode.progressFraction))
                .padding(CardShape.contentInset)
        }
    }

    private var watchState: String {
        if episode.isWatched { return "Watched" }
        guard episode.progressFraction > 0 else { return "" }
        return "\(Int((episode.progressFraction * 100).rounded()))% watched"
    }

    private var heading: String {
        [episode.code, episode.title]
            .filter { !$0.isEmpty }
            .joined(separator: "  ")
    }

    private var details: [String] {
        let upcoming: String? = episode.isReleased ? nil : "Upcoming"
        return [upcoming, episode.releaseDate, episode.runtime].compactMap { $0 }
    }
}
