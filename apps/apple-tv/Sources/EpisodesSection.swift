import SwiftUI
import NuvioTvShared

struct EpisodesSection: View {
    let seasons: [TvosSeason]
    var preferredSeasonNumber: Int32?

    @State private var selectedSeasonNumber: Int32?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            SeasonPicker(seasons: seasons, selectedNumber: selectedSeason?.number) { season in
                selectedSeasonNumber = season.number
            }
            if let season = selectedSeason {
                EpisodeShelf(episodes: season.episodes)
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
    let seasons: [TvosSeason]
    let selectedNumber: Int32?
    let onSelect: (TvosSeason) -> Void
    @FocusState private var focusedNumber: Int32?

    var body: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 24) {
                ForEach(seasons, id: \.number) { season in
                    SeasonButton(
                        title: season.title,
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
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                if isSelected {
                    Image(systemName: "checkmark")
                }
                Text(title)
            }
        }
        .glassButtonStyle()
    }
}

private struct EpisodeShelf: View {
    let episodes: [TvosEpisode]

    var body: some View {
        ScrollView(.horizontal) {
            LazyHStack(alignment: .top, spacing: 40) {
                ForEach(episodes, id: \.videoId) { episode in
                    EpisodeCard(episode: episode)
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

    let episode: TvosEpisode

    var body: some View {
        RouteLink(value: AppRoute.streams(episode.streamTarget)) {
            thumbnail
            captions
        }
        .buttonStyle(.borderless)
        .cardButtonShape()
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
