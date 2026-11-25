import NuvioTvShared
import SwiftUI

enum MpvControl: CaseIterable, Hashable {
    case subtitles
    case audio
    case info

    var label: String {
        switch self {
        case .subtitles: "Subtitles"
        case .audio: "Audio"
        case .info: "Info"
        }
    }

    var symbol: String {
        switch self {
        case .subtitles: "captions.bubble"
        case .audio: "waveform"
        case .info: "info.circle"
        }
    }
}

enum MpvFocus: Hashable {
    case surface
    case control(MpvControl)
    case option(MpvTrackKind, String)
}

enum MpvPanel: Equatable {
    case tracks(MpvTrackKind)
    case info

    var control: MpvControl {
        switch self {
        case let .tracks(kind): kind.control
        case .info: .info
        }
    }
}

struct MpvPlayerScreen: View {
    private static let hideDelay: Duration = .seconds(5)
    private static let skipShown: Duration = .seconds(1)
    private static let infoRefresh: Duration = .seconds(1)
    private static let horizontalInset: CGFloat = 84
    private static let bottomInset: CGFloat = 96
    private static let panelGap: CGFloat = 24
    private static let controlsRise: CGFloat = 28
    private static let controlsAnimation: Animation = .smooth(duration: 0.4)
    private static let panelAnimation: Animation = .spring(response: 0.35, dampingFraction: 0.86)

    private let target: TvosStreamTarget
    private let source: MpvSourceInfo
    private let advisory: PlaybackAdvisory?
    @StateObject private var controller: MpvPlaybackController
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reducesMotion
    @FocusState private var focus: MpvFocus?
    @State private var isControlsShown = true
    @State private var isScrubbing = false
    @State private var resumesAfterScrub = false
    @State private var panel: MpvPanel?
    @State private var trackOptions: [MpvTrackOption] = []
    @State private var infoRows: [MpvInfoRow] = []
    @State private var hideTask: Task<Void, Never>?
    @State private var skip: MpvSkip?
    @State private var skipClearTask: Task<Void, Never>?

    init(request: PlaybackRequest, onFailure: @escaping (StreamNotice) -> Void) {
        target = request.target
        advisory = request.advisory
        source = MpvSourceInfo(stream: request.stream)
        _controller = StateObject(
            wrappedValue: MpvPlaybackController(request: request, onFailure: onFailure)
        )
    }

    var body: some View {
        ZStack {
            surface
            if controller.isBuffering {
                ProgressView()
                    .scaleEffect(1.6)
                    .allowsHitTesting(false)
            }
            if isControlsShown {
                controls
                    .transition(rise(Self.controlsRise))
            }
            if let advisory {
                PlaybackAdvisoryBanner(
                    advisory: advisory,
                    hasPlaybackStarted: controller.hasPlayed
                )
            }
        }
        .ignoresSafeArea()
        .environment(\.colorScheme, .dark)
        .animation(Self.controlsAnimation, value: isControlsShown)
        .animation(.smooth(duration: 0.3), value: isScrubbing)
        .animation(Self.panelAnimation, value: panel)
        .onPlayPauseCommand(perform: togglePause)
        .onExitCommand(perform: back)
        .onAppear {
            controller.start()
            startTouchScrubbing()
            focus = .surface
            scheduleHide()
        }
        .onDisappear(perform: controller.finish)
        .onChange(of: focus) { _, newFocus in
            closePanelIfFocusLeft(newFocus)
            scheduleHide()
        }
        .onChange(of: controller.isPaused) { _, _ in scheduleHide() }
        .onChange(of: isControlsShown) { _, isShown in controller.liftSubtitles(isShown) }
        .task(id: panel) { await refreshInfoWhileOpen() }
    }

    private var surface: some View {
        Button(action: togglePause) {
            MpvVideoView(videoLayer: controller.videoLayer)
        }
        .buttonStyle(MpvSurfaceButtonStyle())
        .focused($focus, equals: .surface)
        .disabled(isTrackMenuOpen)
        .onMoveCommand(perform: moveOnSurface)
        .accessibilityIdentifier("mpv-player")
    }

    private var controls: some View {
        VStack(alignment: .leading, spacing: 34) {
            Spacer(minLength: 0)
            if !isScrubbing {
                infoRow
                    .transition(rise(Self.controlsRise / 2))
            }
            MpvTransportBar(
                clock: controller.clock,
                preview: controller.scrubber.preview,
                isPausedByViewer: controller.isPausedByViewer,
                isScrubbing: isScrubbing,
                skip: skip
            )
                .allowsHitTesting(false)
        }
        .padding(.horizontal, Self.horizontalInset)
        .padding(.bottom, Self.bottomInset)
    }

    private var infoRow: some View {
        HStack(alignment: .bottom) {
            titleBlock
            Spacer(minLength: 40)
            controlButtons
        }
        .overlay(alignment: .topTrailing) {
            panelView
                .alignmentGuide(.top) { dimensions in dimensions[.bottom] + Self.panelGap }
        }
    }

    @ViewBuilder
    private var panelView: some View {
        switch panel {
        case let .tracks(kind):
            MpvTrackMenu(kind: kind, options: trackOptions, focus: $focus) { option in
                choose(option, of: kind)
            }
            .transition(panelTransition)
        case .info:
            MpvInfoMenu(source: source, rows: infoRows)
                .transition(panelTransition)
        case .none:
            EmptyView()
        }
    }

    private var panelTransition: AnyTransition {
        guard !reducesMotion else { return .opacity }
        return .opacity.combined(with: .scale(scale: 0.85, anchor: .bottomTrailing))
    }

    private func rise(_ distance: CGFloat) -> AnyTransition {
        guard !reducesMotion else { return .opacity }
        return .opacity.combined(with: .offset(y: distance))
    }

    private var titleBlock: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let episodeLine = target.subtitle {
                Text(episodeLine)
                    .font(.system(size: 23, weight: .medium))
                    .lineLimit(1)
            }
            Text(target.title)
                .font(.system(size: 54, weight: .bold))
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .foregroundStyle(.white)
        .shadow(color: .black.opacity(0.35), radius: 8)
        .allowsHitTesting(false)
    }

    private var controlButtons: some View {
        HStack(spacing: 20) {
            ForEach(MpvControl.allCases, id: \.self) { control in
                Button { open(control) } label: {
                    Image(systemName: control.symbol)
                        .frame(width: 40, height: 40)
                }
                .mpvCircleButtonStyle(isOpen: panel?.control == control)
                .focused($focus, equals: .control(control))
                .disabled(isTrackMenuOpen && panel?.control != control)
                .accessibilityLabel(control.label)
            }
        }
        .glassGroup(spacing: 20)
        .focusSection()
    }

    private var isTrackMenuOpen: Bool {
        guard case .tracks = panel else { return false }
        return true
    }

    private func togglePause() {
        guard !controller.scrubber.isPending else { return commitScrub() }
        isScrubbing = false
        controller.togglePause()
        showControls()
    }

    private func startTouchScrubbing() {
        let scrubber = controller.scrubber
        scrubber.panContext = {
            MpvPanContext(
                isOnSurface: focus == .surface && panel == nil,
                durationSeconds: controller.clock.durationSeconds
            )
        }
        scrubber.onScrubStart = beginTouchScrub
        scrubber.onSettle = settleScrub
        scrubber.start()
    }

    private func beginTouchScrub() {
        resumesAfterScrub = !controller.isPaused
        controller.setPaused(true)
        isScrubbing = true
        showControls()
    }

    private func commitScrub() {
        seekToScrubPreview()
        endScrub(resuming: true)
    }

    private func settleScrub() {
        seekToScrubPreview()
        endScrub(resuming: resumesAfterScrub)
    }

    private func seekToScrubPreview() {
        if let seconds = controller.scrubber.commit() {
            controller.seek(toSeconds: seconds)
        }
    }

    private func cancelScrub() {
        controller.scrubber.cancel()
        endScrub(resuming: resumesAfterScrub)
    }

    private func endScrub(resuming: Bool) {
        isScrubbing = false
        if resuming { controller.setPaused(false) }
        showControls()
    }

    private func moveOnSurface(_ direction: MoveCommandDirection) {
        guard isControlsShown else {
            seekIfHorizontal(direction)
            showControls()
            return
        }
        switch direction {
        case .left, .right:
            seekIfHorizontal(direction)
        case .down:
            isScrubbing = true
        case .up where controller.scrubber.isPending:
            break
        case .up where isScrubbing:
            isScrubbing = false
        case .up:
            focus = .control(.subtitles)
        default:
            break
        }
        showControls()
    }

    private func seekIfHorizontal(_ direction: MoveCommandDirection) {
        let scrubber = controller.scrubber
        guard !scrubber.isSwiping else { return }
        guard !scrubber.isPending else { return stepPreview(direction) }
        switch direction {
        case .left:
            isScrubbing = true
            controller.seek(bySteps: -1)
            showSkip(isForward: false)
        case .right:
            isScrubbing = true
            controller.seek(bySteps: 1)
            showSkip(isForward: true)
        default:
            break
        }
    }

    private func stepPreview(_ direction: MoveCommandDirection) {
        switch direction {
        case .left: controller.scrubber.step(by: -1)
        case .right: controller.scrubber.step(by: 1)
        default: break
        }
    }

    private func showSkip(isForward: Bool) {
        skip = skip?.adding(isForward: isForward) ?? MpvSkip(isForward: isForward, presses: 1)
        skipClearTask?.cancel()
        skipClearTask = Task { @MainActor in
            try? await Task.sleep(for: Self.skipShown)
            guard !Task.isCancelled else { return }
            skip = nil
        }
    }

    private func open(_ control: MpvControl) {
        guard panel?.control != control else {
            closePanel()
            return
        }
        switch control {
        case .subtitles:
            openTracks(.subtitles)
        case .audio:
            openTracks(.audio)
        case .info:
            infoRows = controller.mediaInfo()
            panel = .info
        }
    }

    private func openTracks(_ kind: MpvTrackKind) {
        trackOptions = controller.trackOptions(kind)
        panel = .tracks(kind)
        let selected = trackOptions.first(where: \.isSelected) ?? trackOptions.first
        guard let selected else { return }
        DispatchQueue.main.async { focus = .option(kind, selected.id) }
    }

    private func choose(_ option: MpvTrackOption, of kind: MpvTrackKind) {
        controller.select(option, of: kind)
        closePanel()
    }

    private func closePanel() {
        guard let closing = panel else { return }
        panel = nil
        DispatchQueue.main.async { focus = .control(closing.control) }
    }

    private func closePanelIfFocusLeft(_ newFocus: MpvFocus?) {
        guard let open = panel, let newFocus else { return }
        switch newFocus {
        case let .option(kind, _) where open == .tracks(kind):
            return
        case .control(open.control):
            return
        default:
            panel = nil
        }
    }

    private func back() {
        if panel != nil {
            closePanel()
        } else if controller.scrubber.isPending {
            cancelScrub()
        } else if isScrubbing {
            isScrubbing = false
            showControls()
        } else if focus != .surface, isControlsShown {
            focus = .surface
        } else if isControlsShown {
            isControlsShown = false
        } else {
            dismiss()
        }
    }

    private func showControls() {
        isControlsShown = true
        scheduleHide()
    }

    private func scheduleHide() {
        hideTask?.cancel()
        hideTask = Task { @MainActor in
            try? await Task.sleep(for: Self.hideDelay)
            guard !Task.isCancelled, canHideControls else { return }
            hideControls()
        }
    }

    private var canHideControls: Bool {
        !controller.isPaused && panel == nil && focus != nil && !controller.scrubber.isPending
    }

    private func hideControls() {
        focus = .surface
        isScrubbing = false
        isControlsShown = false
    }

    private func refreshInfoWhileOpen() async {
        while panel == .info, !Task.isCancelled {
            infoRows = controller.mediaInfo()
            try? await Task.sleep(for: Self.infoRefresh)
        }
    }
}

private struct MpvSurfaceButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
    }
}
