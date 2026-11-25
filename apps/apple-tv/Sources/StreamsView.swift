import SwiftUI
import NuvioTvShared

/// Streams for one movie or episode: the title column on the left stays in view, the list of
/// sources scrolls on the right.
struct StreamsView: View {
    private static let columnSpacing: CGFloat = 40

    @StateObject private var model: StreamsModel
    @StateObject private var playback: StreamsPlayback
    @Environment(\.accessibilityReduceMotion) private var reducesMotion
    @Environment(\.dismiss) private var dismiss

    init(target: TvosStreamTarget, resumesDirectly: Bool = false) {
        _model = StateObject(wrappedValue: StreamsModel(target: target))
        _playback = StateObject(wrappedValue: StreamsPlayback(
            target: target,
            resumesDirectly: resumesDirectly
        ))
    }

    var body: some View {
        HStack(alignment: .top, spacing: Self.columnSpacing) {
            StreamsHeader(
                target: model.target,
                resume: playback.directResume,
                sourceCount: model.sourceCount,
                searchingAddonCount: model.searchingAddonCount
            )
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .animation(StreamsMotion.animation(reducesMotion: reducesMotion), value: model.phase)
                .disabled(playback.isOpeningLastLink)
        }
        .background { StreamsBackground(backdropUrl: model.target.backdropUrl) }
        .overlay {
            if playback.isOpeningLastLink { StreamsOpeningCover() }
        }
        .sheet(item: $playback.sheet) { sheet in
            StreamDetailSheet(stream: sheet.stream, notice: sheet.notice)
        }
        .confirmationDialog(
            model.target.title,
            isPresented: isResumeChoiceShown,
            titleVisibility: .hidden,
            presenting: playback.pendingResume
        ) { pending in
            Button(pending.point.label) {
                playback.start(pending.stream, resumes: true, engine: pending.engine)
            }
            Button(pending.point.startOverLabel) {
                playback.start(pending.stream, resumes: false, engine: pending.engine)
            }
        }
        .fullScreenCover(item: $playback.playback, onDismiss: closePlayer) { request in
            PlaybackScreen(request: request) { notice in
                playback.fail(request, notice: notice)
            }
            .task { await playback.clearSelectionOncePlayerCovers() }
        }
        .onAppear {
            model.observe()
            playback.loadDirectResume()
            playback.playLastLink()
        }
        .onDisappear {
            model.stop()
            playback.stop()
        }
        #if DEBUG
        .onChange(of: model.items) { _, items in openDebugStream(in: items) }
        #endif
    }

    @ViewBuilder
    private var content: some View {
        switch model.phase {
        case .empty:
            if let emptyState = model.emptyState {
                StreamsEmptyView(emptyState: emptyState, onRetry: model.reload)
                    .transition(.opacity)
            }
        case .loading:
            StreamsLoadingView()
                .transition(.opacity)
        case .list:
            StreamGroupList(
                items: model.items,
                checkingStreamKey: playback.checkingStreamKey,
                torrent: playback.torrent,
                onSelect: playback.select
            )
            .transition(.opacity)
        }
    }

    private func closePlayer() {
        if playback.afterPlayback() { dismiss() }
    }

    private var isResumeChoiceShown: Binding<Bool> {
        Binding(
            get: { playback.pendingResume != nil },
            set: { isShown in if !isShown { playback.pendingResume = nil } }
        )
    }

    #if DEBUG
    private func openDebugStream(in items: [StreamListItem]) {
        guard DebugLaunch.opensFirstStream, playback.sheet == nil, playback.playback == nil else {
            return
        }
        for case let .stream(row) in items {
            DebugLaunch.markFirstStreamOpened()
            playback.select(row.stream)
            return
        }
    }
    #endif
}
