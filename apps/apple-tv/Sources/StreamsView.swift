import SwiftUI
import NuvioTvShared

extension TvosStream: Identifiable {
    public var id: String { key }
}

struct StreamSheet: Identifiable {
    let stream: TvosStream
    let notice: StreamNotice?

    var id: String { stream.key + (notice?.title ?? "") }
}

struct StreamsView: View {
    @StateObject private var model: StreamsModel
    @State private var sheet: StreamSheet?
    @State private var pendingResume: PendingResume?
    @State private var playback: PlaybackRequest?
    @State private var playbackFailure: StreamSheet?
    @State private var checkingStreamKey: String?
    @State private var directResume: TvosResumePoint?
    @StateObject private var torrent = TorrentSession()

    private let resumesDirectly: Bool

    init(target: TvosStreamTarget, resumesDirectly: Bool = false) {
        _model = StateObject(wrappedValue: StreamsModel(target: target))
        self.resumesDirectly = resumesDirectly
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 36) {
            StreamsHeader(
                target: model.target,
                resume: directResume,
                isChecking: checkingStreamKey != nil,
                torrentStatus: torrent.startingStreamKey == nil ? nil : torrent.status
            )
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
        .animation(.smooth, value: checkingStreamKey)
        .background { StreamsBackground(backdropUrl: model.target.backdropUrl) }
        .sheet(item: $sheet) { sheet in
            StreamDetailSheet(stream: sheet.stream, notice: sheet.notice)
        }
        .confirmationDialog(
            model.target.title,
            isPresented: isResumeChoiceShown,
            titleVisibility: .visible,
            presenting: pendingResume
        ) { pending in
            Button(pending.point.label) {
                start(pending.stream, resumes: true, engine: pending.engine)
            }
            Button(pending.point.startOverLabel) {
                start(pending.stream, resumes: false, engine: pending.engine)
            }
        }
        .fullScreenCover(item: $playback, onDismiss: afterPlayback) { request in
            PlaybackScreen(request: request) { notice in
                fail(request, notice: notice)
            }
        }
        .onAppear {
            model.observe()
            loadDirectResume()
        }
        .onDisappear {
            model.stop()
            torrent.stop()
        }
        #if DEBUG
        .onChange(of: model.groups) { _, groups in openDebugStream(in: groups) }
        #endif
    }

    @ViewBuilder
    private var content: some View {
        if let emptyState = model.emptyState {
            StreamsEmptyView(emptyState: emptyState, onRetry: model.reload)
        } else if model.groups.isEmpty {
            StreamsLoadingView()
        } else {
            StreamGroupList(groups: model.groups, onSelect: select)
        }
    }

    private var isResumeChoiceShown: Binding<Bool> {
        Binding(
            get: { pendingResume != nil },
            set: { isShown in if !isShown { pendingResume = nil } }
        )
    }

    private func loadDirectResume() {
        guard resumesDirectly else { return }
        TvosPlayback.shared.resumePoint(target: model.target) { point in
            directResume = point
        }
    }

    private func select(_ stream: TvosStream) {
        guard checkingStreamKey == nil else { return }
        if stream.playableUrl == nil, stream.infoHash != nil {
            startTorrent(stream)
            return
        }
        guard let probe = PlaybackRequest(target: model.target, stream: stream, resumes: false)
        else {
            sheet = StreamSheet(stream: stream, notice: nil)
            return
        }
        checkingStreamKey = stream.key
        preflight(probe)
    }

    private func startTorrent(_ stream: TvosStream) {
        checkingStreamKey = stream.key
        torrent.start(stream) { result in
            switch result {
            case let .success(url):
                preflight(PlaybackRequest(
                    target: model.target,
                    stream: stream,
                    url: url,
                    resumes: false
                ))
            case let .failure(error):
                checkingStreamKey = nil
                sheet = StreamSheet(stream: stream, notice: torrentNotice(error))
            }
        }
    }

    private func torrentNotice(_ error: TorrentStartError) -> StreamNotice {
        StreamNotice(title: "The torrent did not start", message: error.message)
    }

    private func preflight(_ probe: PlaybackRequest) {
        let stream = probe.stream
        Task { @MainActor in
            let engine = await PlaybackPreflight.engine(for: probe)
            checkingStreamKey = nil
            chooseStart(stream, engine: engine)
        }
    }

    private func chooseStart(_ stream: TvosStream, engine: PlayerEngine) {
        guard !resumesDirectly else {
            start(stream, resumes: true, engine: engine)
            return
        }
        TvosPlayback.shared.resumePoint(target: model.target) { point in
            if let point {
                offerResume(PendingResume(stream: stream, point: point, engine: engine))
            } else {
                start(stream, resumes: false, engine: engine)
            }
        }
    }

    private func offerResume(_ pending: PendingResume) {
        #if DEBUG
        if let resumes = DebugLaunch.resumeChoice {
            start(pending.stream, resumes: resumes, engine: pending.engine)
            return
        }
        #endif
        pendingResume = pending
    }

    private func start(_ stream: TvosStream, resumes: Bool, engine: PlayerEngine) {
        pendingResume = nil
        playback = playbackRequest(for: stream, resumes: resumes, engine: engine)
    }

    private func playbackRequest(
        for stream: TvosStream,
        resumes: Bool,
        engine: PlayerEngine
    ) -> PlaybackRequest? {
        guard let torrentUrl = torrent.localUrl(for: stream) else {
            return PlaybackRequest(
                target: model.target,
                stream: stream,
                resumes: resumes,
                engine: engine
            )
        }
        return PlaybackRequest(
            target: model.target,
            stream: stream,
            url: torrentUrl,
            resumes: resumes,
            engine: engine
        )
    }

    private func fail(_ request: PlaybackRequest, notice: StreamNotice) {
        playbackFailure = StreamSheet(stream: request.stream, notice: notice)
        playback = nil
    }

    private func afterPlayback() {
        torrent.stop()
        loadDirectResume()
        guard let failure = playbackFailure else { return }
        playbackFailure = nil
        sheet = failure
    }

    #if DEBUG
    private func openDebugStream(in groups: [TvosStreamGroup]) {
        guard DebugLaunch.opensFirstStream, sheet == nil, playback == nil,
              let stream = groups.first(where: { !$0.streams.isEmpty })?.streams.first
        else { return }
        DebugLaunch.markFirstStreamOpened()
        select(stream)
    }
    #endif
}

private struct StreamsBackground: View {
    let backdropUrl: String?

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.13, green: 0.14, blue: 0.17),
                    Color(red: 0.08, green: 0.09, blue: 0.1),
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            BackdropView(url: backdropUrl, isReceded: true)
        }
        .ignoresSafeArea()
    }
}
