import NuvioTvShared
import SwiftUI

struct HomeHero: View {
    static let height: CGFloat = 880
    private static let advanceInterval: Duration = .seconds(8)
    private static let maxItems = 6

    let items: [TvosItem]
    @State private var index = 0
    @State private var advanceTask: Task<Void, Never>?

    init(rows: [TvosRow]) {
        let featured = rows.first?.items.filter { $0.backdropUrl != nil } ?? []
        items = Array(featured.prefix(Self.maxItems))
    }

    var body: some View {
        if let item = currentItem {
            ZStack(alignment: .bottomLeading) {
                HeroBackdrop(url: item.backdropUrl)
                    .id(item.metaId)
                    .transition(.opacity)
                HeroDetails(item: item, onMove: move)
                    .padding(.horizontal, 80)
                    .padding(.bottom, 70)
                PageIndicator(count: items.count, index: index)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .padding(.horizontal, 80)
                    .padding(.bottom, 80)
            }
            .frame(height: Self.height)
            .animation(.easeInOut(duration: 0.6), value: index)
            .onAppear(perform: scheduleAdvance)
            .onDisappear { advanceTask?.cancel() }
        }
    }

    private var currentItem: TvosItem? {
        items.indices.contains(index) ? items[index] : items.first
    }

    private func move(_ direction: MoveCommandDirection) {
        switch direction {
        case .left:
            show(index - 1)
        case .right:
            show(index + 1)
        default:
            break
        }
    }

    private func show(_ newIndex: Int) {
        guard !items.isEmpty else { return }
        index = (newIndex + items.count) % items.count
        scheduleAdvance()
    }

    private func scheduleAdvance() {
        advanceTask?.cancel()
        advanceTask = Task { @MainActor in
            try? await Task.sleep(for: Self.advanceInterval)
            guard !Task.isCancelled else { return }
            show(index + 1)
        }
    }
}

private struct HeroBackdrop: View {
    let url: String?

    var body: some View {
        Color.clear
            .overlay { ArtworkImage(url: url) }
            .clipped()
            .overlay(
                LinearGradient(
                    stops: [
                        .init(color: .black.opacity(0.75), location: 0),
                        .init(color: .black.opacity(0.25), location: 0.5),
                        .init(color: .clear, location: 0.75),
                    ],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .mask(
                LinearGradient(
                    stops: [
                        .init(color: .black, location: 0.55),
                        .init(color: .clear, location: 1),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .ignoresSafeArea(edges: [.top, .horizontal])
    }
}

private struct HeroDetails: View {
    let item: TvosItem
    let onMove: (MoveCommandDirection) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 22) {
            HeroTitle(name: item.name, logoUrl: item.logoUrl)
            if !metadataLine.isEmpty {
                Text(metadataLine)
                    .font(.callout.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.8))
            }
            if let overview = item.overview {
                Text(overview)
                    .font(.callout)
                    .foregroundStyle(.white.opacity(0.85))
                    .lineLimit(3)
            }
            NavigationLink(value: AppRoute.details(type: item.type, id: item.metaId)) {
                Label("Watch Now", systemImage: "play.fill")
                    .font(.headline)
                    .padding(.horizontal, 12)
            }
            .glassProminentButtonStyle()
            .onMoveCommand(perform: onMove)
            .accessibilityIdentifier("homeHeroButton")
            .padding(.top, 6)
        }
        .frame(maxWidth: 860, alignment: .leading)
        .focusSection()
    }

    private var metadataLine: String {
        let kind = item.type == "series" ? "TV Show" : "Movie"
        let rating = item.imdbRating.map { "IMDb \($0)" }
        let genres = item.genres.prefix(2).joined(separator: ", ")
        return [kind, item.releaseInfo, genres.isEmpty ? nil : genres, rating]
            .compactMap { $0 }
            .joined(separator: " · ")
    }
}

private struct HeroTitle: View {
    let name: String
    let logoUrl: String?

    var body: some View {
        AsyncImage(url: logoUrl.flatMap(URL.init(string:))) { phase in
            if let image = phase.image {
                image.resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(maxWidth: 620, maxHeight: 200, alignment: .bottomLeading)
            } else {
                Text(name)
                    .font(.system(size: 76, weight: .bold))
                    .lineLimit(2)
            }
        }
        .accessibilityLabel(name)
    }
}

private struct PageIndicator: View {
    let count: Int
    let index: Int

    var body: some View {
        HStack(spacing: 10) {
            ForEach(0..<count, id: \.self) { position in
                Capsule()
                    .fill(.white.opacity(position == index ? 1 : 0.35))
                    .frame(width: position == index ? 28 : 10, height: 10)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .glassPanel(cornerRadius: 20)
        .opacity(count > 1 ? 1 : 0)
    }
}
