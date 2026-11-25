import NuvioTvShared
import SwiftUI

struct HomeHero: View {
    static let contentInset: CGFloat = 80
    private static let nextShelfPeek: CGFloat = 150
    private static let advanceInterval: Duration = .seconds(8)
    private static let maxItems = 6
    static let placeholderFocusId = "home-hero-placeholder"

    let items: [TvosItem]
    let isLoading: Bool
    /// How far the artwork runs below the spotlight: the next shelf peek and the bottom safe area,
    /// so it reaches the bottom of the screen.
    let artExtension: CGFloat
    let onFocus: () -> Void
    @State private var currentId: String?
    @State private var advanceTask: Task<Void, Never>?
    @State private var hasTakenLaunchFocus = false
    @StateObject private var trailer = HeroTrailer()
    @EnvironmentObject private var router: Router
    @FocusState private var focusedId: String?

    init(rows: [TvosRow], isLoading: Bool, bottomInset: CGFloat, onFocus: @escaping () -> Void) {
        var rows = rows
        var isLoading = isLoading
        #if DEBUG
        if DebugLaunch.holdsHomeHeroPlaceholder {
            rows = []
            isLoading = true
        }
        #endif
        self.isLoading = isLoading
        artExtension = Self.nextShelfPeek + bottomInset
        self.onFocus = onFocus
        let featured = rows.first?.items.filter { $0.backdropUrl != nil } ?? []
        items = Array(featured.prefix(Self.maxItems))
    }

    var body: some View {
        if !items.isEmpty {
            pages
                .overlay {
                    // From the screen edge, not the safe area, like the pages.
                    HeroForeground(items: items, currentIndex: currentIndex, focusedId: $focusedId)
                        .ignoresSafeArea(edges: .horizontal)
                }
                .overlay(alignment: .bottom) {
                    PageIndicator(count: items.count, index: currentIndex)
                        .padding(.bottom, 36)
                }
                .containerRelativeFrame(.vertical) { height, _ in height - Self.nextShelfPeek }
                .onAppear(perform: start)
                .onDisappear {
                    advanceTask?.cancel()
                    trailer.stop()
                }
                .onChange(of: focusedId) { _, id in follow(id) }
                .onChange(of: currentId) { prefetchTrailers() }
                // Up from any card in the first shelf lands here, not only from the cards below the
                // button, and on the page that is showing.
                .focusSection()
                .defaultFocus($focusedId, currentId ?? items.first?.metaId)
        } else if isLoading {
            // The pages run to the screen edges, so the placeholder does too and its inset matches.
            HeroPlaceholder(focusedId: $focusedId)
                .ignoresSafeArea(edges: .horizontal)
                .containerRelativeFrame(.vertical) { height, _ in height - Self.nextShelfPeek }
                .defaultFocus($focusedId, Self.placeholderFocusId)
                .onAppear(perform: holdLaunchFocus)
        }
    }

    private var pages: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 0) {
                ForEach(items, id: \.metaId) { item in
                    HeroPage(item: item, trailer: trailer.picture(for: item.metaId), artExtension: artExtension)
                        .containerRelativeFrame(.horizontal)
                        .id(item.metaId)
                }
            }
            .scrollTargetLayout()
        }
        .scrollPosition(id: $currentId)
        .scrollTargetBehavior(.paging)
        .scrollIndicators(.hidden)
        .scrollClipDisabled()
        .ignoresSafeArea(edges: .horizontal)
    }

    private var currentIndex: Int {
        items.firstIndex { $0.metaId == currentId } ?? 0
    }

    private func start() {
        if currentId == nil { currentId = items.first?.metaId }
        takeLaunchFocus()
        scheduleAdvance()
        prefetchTrailers()
    }

    /// The page showing and the next one, so either is ready by the time it gets focus.
    private func prefetchTrailers() {
        guard !items.isEmpty else { return }
        let next = items[(currentIndex + 1) % items.count]
        trailer.prefetch([items[currentIndex], next])
    }

    /// Keeps launch focus off the sidebar while Home loads. The real hero takes it over later.
    private func holdLaunchFocus() {
        guard !hasTakenLaunchFocus, router.path.isEmpty else { return }
        DispatchQueue.main.async { focusedId = Self.placeholderFocusId }
    }

    private func takeLaunchFocus() {
        guard !hasTakenLaunchFocus, router.path.isEmpty, let first = items.first?.metaId
        else { return }
        hasTakenLaunchFocus = true
        DispatchQueue.main.async { focusedId = first }
    }

    private func follow(_ id: String?) {
        if id != nil { onFocus() }
        trailer.focus(items.first { $0.metaId == id })
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
