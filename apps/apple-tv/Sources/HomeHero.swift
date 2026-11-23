import NuvioTvShared
import SwiftUI

struct HomeHero: View {
    static let contentInset: CGFloat = 80
    private static let nextShelfPeek: CGFloat = 150
    private static let advanceInterval: Duration = .seconds(8)
    private static let maxItems = 6

    let items: [TvosItem]
    let onFocus: () -> Void
    @State private var currentId: String?
    @State private var advanceTask: Task<Void, Never>?
    @State private var hasTakenLaunchFocus = false
    @EnvironmentObject private var router: Router
    @FocusState private var focusedId: String?

    init(rows: [TvosRow], onFocus: @escaping () -> Void) {
        self.onFocus = onFocus
        let featured = rows.first?.items.filter { $0.backdropUrl != nil } ?? []
        items = Array(featured.prefix(Self.maxItems))
    }

    var body: some View {
        if !items.isEmpty {
            pages
                .overlay(alignment: .bottom) {
                    PageIndicator(count: items.count, index: currentIndex)
                        .padding(.bottom, 36)
                }
                .containerRelativeFrame(.vertical) { height, _ in height - Self.nextShelfPeek }
                .onAppear(perform: start)
                .onDisappear { advanceTask?.cancel() }
                .onChange(of: focusedId) { _, id in follow(id) }
                .defaultFocus($focusedId, items.first?.metaId)
        }
    }

    private var pages: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 0) {
                ForEach(items, id: \.metaId) { item in
                    HeroPage(item: item, focusedId: $focusedId)
                        .containerRelativeFrame(.horizontal)
                        .id(item.metaId)
                }
            }
            .scrollTargetLayout()
        }
        .scrollPosition(id: $currentId)
        .scrollTargetBehavior(.paging)
        .scrollIndicators(.hidden)
        .ignoresSafeArea(edges: .horizontal)
    }

    private var currentIndex: Int {
        items.firstIndex { $0.metaId == currentId } ?? 0
    }

    private func start() {
        if currentId == nil { currentId = items.first?.metaId }
        takeLaunchFocus()
        scheduleAdvance()
    }

    private func takeLaunchFocus() {
        guard !hasTakenLaunchFocus, router.path.isEmpty, let first = items.first?.metaId
        else { return }
        hasTakenLaunchFocus = true
        DispatchQueue.main.async { focusedId = first }
    }

    private func follow(_ id: String?) {
        if id != nil { onFocus() }
        if let id, id != currentId {
            withAnimation(.smooth(duration: 0.5)) { currentId = id }
        }
        scheduleAdvance()
    }

    private func scheduleAdvance() {
        advanceTask?.cancel()
        advanceTask = Task { @MainActor in
            try? await Task.sleep(for: Self.advanceInterval)
            guard !Task.isCancelled else { return }
            advance()
        }
    }

    private func advance() {
        if focusedId == nil {
            let next = items[(currentIndex + 1) % items.count]
            withAnimation(.smooth(duration: 0.8)) { currentId = next.metaId }
        }
        scheduleAdvance()
    }
}

private struct HeroPage: View {
    let item: TvosItem
    let focusedId: FocusState<String?>.Binding

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            HeroBackdrop(url: item.backdropUrl)
            HeroDetails(item: item, focusedId: focusedId)
                .padding(.horizontal, HomeHero.contentInset)
                .padding(.bottom, 70)
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
            .ignoresSafeArea(edges: .top)
    }
}

private struct HeroDetails: View {
    let item: TvosItem
    let focusedId: FocusState<String?>.Binding

    var body: some View {
        VStack(alignment: .leading, spacing: 22) {
            HeroTitle(name: item.name, logoUrl: item.logoUrl)
            MetadataLine(facts: facts, imdbRating: item.imdbRating, badgeFontSize: 24)
                .font(.callout.weight(.semibold))
                .foregroundStyle(.white.opacity(0.8))
            if let overview = item.overview {
                Text(overview)
                    .font(.callout)
                    .foregroundStyle(.white.opacity(0.85))
                    .lineLimit(3)
            }
            RouteLink(value: AppRoute.details(type: item.type, id: item.metaId)) {
                Label("Watch Now", systemImage: "play.fill")
                    .font(.body.weight(.semibold))
                    .padding(.horizontal, 8)
            }
            .glassProminentButtonStyle()
            .controlSize(.small)
            .focused(focusedId, equals: item.metaId)
            .accessibilityIdentifier("homeHeroButton")
            .accessibilityValue(item.name)
            .padding(.top, 6)
        }
        .frame(maxWidth: 860, alignment: .leading)
    }

    private var facts: [String] {
        let kind = item.type == "series" ? "TV Show" : "Movie"
        let genres = item.genres.prefix(2).joined(separator: ", ")
        return [kind, item.releaseInfo, genres.isEmpty ? nil : genres].compactMap { $0 }
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
