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
            LazyHStack(alignment: .top, spacing: 48) {
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
    static let width: CGFloat = 360

    let episode: TvosEpisode

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            NavigationLink(value: AppRoute.streams(episode.streamTarget)) {
                ArtworkImage(url: episode.thumbnailUrl)
                    .frame(width: Self.width, height: Self.width * 9 / 16)
                    .opacity(episode.isReleased ? 1 : 0.5)
                    .overlay(alignment: .topTrailing) { watchedMark }
                    .overlay(alignment: .bottom) { progressBar }
            }
            .buttonStyle(.card)
            .accessibilityIdentifier("episode-\(episode.videoId)")
            .accessibilityLabel(heading)
            .accessibilityValue(watchState)
            Text(heading)
                .font(.callout)
                .lineLimit(1)
            if !detailLine.isEmpty {
                Text(detailLine)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
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
                .padding(16)
        }
    }

    @ViewBuilder
    private var progressBar: some View {
        if !episode.isWatched && episode.progressFraction > 0 {
            WatchProgressBar(fraction: Double(episode.progressFraction))
                .padding(16)
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

    private var detailLine: String {
        let upcoming: String? = episode.isReleased ? nil : "Upcoming"
        return [upcoming, episode.releaseDate, episode.runtime]
            .compactMap { $0 }
            .joined(separator: "  ·  ")
    }
}
