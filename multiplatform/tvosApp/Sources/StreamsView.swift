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
                    isChecking: checkingStreamKey != nil
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
            Button(pending.point.label) { start(pending.stream, resumes: true) }
            Button(pending.point.startOverLabel) { start(pending.stream, resumes: false) }
        }
        .fullScreenCover(item: $playback, onDismiss: afterPlayback) { request in
            PlayerView(request: request) { notice in
                fail(request, notice: notice)
            }
            .ignoresSafeArea()
        }
        .onAppear {
            model.observe()
            loadDirectResume()
        }
        .onDisappear(perform: model.stop)
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
        guard let probe = PlaybackRequest(target: model.target, stream: stream, resumes: false)
        else {
            sheet = StreamSheet(stream: stream, notice: nil)
            return
        }
        checkingStreamKey = stream.key
        Task { @MainActor in
            let verdict = await PlaybackPreflight.check(probe)
            checkingStreamKey = nil
            switch verdict {
            case .playable:
                chooseStart(stream)
            case let .unplayable(notice):
                sheet = StreamSheet(stream: stream, notice: notice)
            }
        }
    }

    private func chooseStart(_ stream: TvosStream) {
        guard !resumesDirectly else {
            start(stream, resumes: true)
            return
        }
        TvosPlayback.shared.resumePoint(target: model.target) { point in
            if let point {
                offerResume(PendingResume(stream: stream, point: point))
            } else {
                start(stream, resumes: false)
            }
        }
    }

    private func offerResume(_ pending: PendingResume) {
        #if DEBUG
        if let resumes = DebugLaunch.resumeChoice {
            start(pending.stream, resumes: resumes)
            return
        }
        #endif
        pendingResume = pending
    }

    private func start(_ stream: TvosStream, resumes: Bool) {
        pendingResume = nil
        playback = PlaybackRequest(target: model.target, stream: stream, resumes: resumes)
    }

    private func fail(_ request: PlaybackRequest, notice: StreamNotice) {
        playbackFailure = StreamSheet(stream: request.stream, notice: notice)
        playback = nil
    }

    private func afterPlayback() {
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
            if isChecking {
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
