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
        ZStack {
            Color.black.ignoresSafeArea()
            BackdropView(url: model.target.backdropUrl, dimming: 0.7)
            HStack(alignment: .top, spacing: 80) {
                StreamsHeader(
                    target: model.target,
                    resume: directResume,
                    isChecking: checkingStreamKey != nil,
                    torrentStatus: torrent.startingStreamKey == nil ? nil : torrent.status
                )
                .frame(width: 560, alignment: .leading)
                content
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            }
            .padding(.top, 40)
        }
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

private struct StreamsHeader: View {
    let target: TvosStreamTarget
    let resume: TvosResumePoint?
    let isChecking: Bool
    let torrentStatus: TvosTorrentStatus?

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text(target.title)
                .font(.title2.bold())
                .lineLimit(3)
            if let subtitle = target.subtitle {
                Text(subtitle)
                    .font(.headline)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            if let resume {
                VStack(alignment: .leading, spacing: 12) {
                    Text(resume.label)
                        .font(.headline)
                    WatchProgressBar(fraction: Double(resume.watchedFraction))
                        .frame(width: 360)
                }
                .padding(.vertical, 8)
            }
            Text("Streams")
                .font(.callout)
                .foregroundStyle(.secondary)
            if let torrentStatus {
                TorrentProgressView(status: torrentStatus)
            } else if isChecking {
                CheckingStreamView()
            }
        }
    }
}

private struct CheckingStreamView: View {
    var body: some View {
        HStack(spacing: 24) {
            ProgressView()
            Text("Checking stream")
                .font(.headline)
        }
        .padding(.horizontal, 32)
        .padding(.vertical, 24)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24))
        .padding(.top, 20)
    }
}

private struct StreamsLoadingView: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 24) {
            ProgressView()
            Text("Finding streams")
                .font(.headline)
                .foregroundStyle(.secondary)
        }
    }
}

private struct TorrentProgressView: View {
    let status: TvosTorrentStatus

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 24) {
                ProgressView()
                Text(status.step ?? "Starting torrent")
                    .font(.headline)
                    .lineLimit(2)
            }
            Text(details)
                .font(.callout)
                .foregroundStyle(.secondary)
            ProgressView(value: Double(status.bufferFraction))
                .frame(width: 360)
        }
        .padding(.horizontal, 32)
        .padding(.vertical, 24)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24))
        .padding(.top, 20)
    }

    private var details: String {
        "\(status.peers) peers, \(status.seeds) seeds, \(status.downloadSpeedLabel)"
    }
}
