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
}

struct MpvPlayerScreen: View {
    private static let hideDelay: Duration = .seconds(5)
    private static let infoRefresh: Duration = .seconds(1)
    private static let horizontalInset: CGFloat = 84
    private static let bottomInset: CGFloat = 96
    private static let panelGap: CGFloat = 24
    private static let controlsRise: CGFloat = 28
    private static let controlsAnimation: Animation = .smooth(duration: 0.4)

    private let target: TvosStreamTarget
    private let subtitleLine: String
    private let source: String
    @StateObject private var controller: MpvPlaybackController
    @Environment(\.dismiss) private var dismiss
    @FocusState private var focus: MpvFocus?
    @State private var isControlsShown = true
    @State private var isScrubbing = false
    @State private var isInfoOpen = false
    @State private var subtitleOptions: [MpvTrackOption] = []
    @State private var audioOptions: [MpvTrackOption] = []
    @State private var infoRows: [MpvInfoRow] = []
    @State private var hideTask: Task<Void, Never>?

    init(request: PlaybackRequest, onFailure: @escaping (StreamNotice) -> Void) {
        target = request.target
        subtitleLine = request.target.subtitle
            ?? [request.stream.name, request.stream.addonName].joined(separator: "  ·  ")
        source = [request.stream.addonName, request.stream.filename ?? request.stream.name]
            .joined(separator: " · ")
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
                scrim
                    .transition(.opacity)
                controls
                    .transition(.opacity.combined(with: .offset(y: Self.controlsRise)))
            }
        }
        .ignoresSafeArea()
        .animation(Self.controlsAnimation, value: isControlsShown)
        .animation(.smooth(duration: 0.3), value: isScrubbing)
        .animation(.smooth(duration: 0.3), value: isInfoOpen)
        .onPlayPauseCommand(perform: togglePause)
        .onExitCommand(perform: back)
        .onAppear {
            controller.start()
            focus = .surface
            scheduleHide()
        }
        .onDisappear(perform: controller.finish)
        .onChange(of: focus) { _, newFocus in
            closeInfoIfFocusLeft(newFocus)
            refreshTracksIfOnControl(newFocus)
            scheduleHide()
        }
        .onChange(of: controller.isPaused) { _, _ in scheduleHide() }
        .onChange(of: isControlsShown) { _, isShown in controller.liftSubtitles(isShown) }
        .task(id: isInfoOpen) { await refreshInfoWhileOpen() }
    }

    private var surface: some View {
        Button(action: togglePause) {
            MpvVideoView(videoLayer: controller.videoLayer)
        }
        .buttonStyle(MpvSurfaceButtonStyle())
        .focused($focus, equals: .surface)
        .onMoveCommand(perform: moveOnSurface)
        .accessibilityIdentifier("mpv-player")
    }

    private var scrim: some View {
        LinearGradient(
            colors: [.clear, .black.opacity(0.55)],
            startPoint: UnitPoint(x: 0.5, y: 0.55),
            endPoint: .bottom
        )
        .allowsHitTesting(false)
    }

    private var controls: some View {
        VStack(alignment: .leading, spacing: 34) {
            Spacer(minLength: 0)
            if !isScrubbing {
                infoRow
                    .transition(.opacity.combined(with: .offset(y: Self.controlsRise / 2)))
            }
            MpvTransportBar(controller: controller, isScrubbing: isScrubbing)
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
        if isInfoOpen {
            MpvInfoMenu(source: source, rows: infoRows)
                .transition(.opacity.combined(with: .scale(scale: 0.9, anchor: .bottomTrailing)))
        }
    }

    private var titleBlock: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(subtitleLine)
                .font(.system(size: 23, weight: .medium))
                .lineLimit(1)
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
            trackMenu(.subtitles, options: subtitleOptions)
            trackMenu(.audio, options: audioOptions)
            Button(action: toggleInfo) {
                Image(systemName: MpvControl.info.symbol)
            }
            .buttonStyle(MpvCircleButtonStyle(isOpen: isInfoOpen))
            .focused($focus, equals: .control(.info))
            .accessibilityLabel(MpvControl.info.label)
        }
        .focusSection()
    }

    private func trackMenu(_ kind: MpvTrackKind, options: [MpvTrackOption]) -> some View {
        MpvTrackMenu(kind: kind, options: options) { option in
            choose(option, of: kind)
        }
        .focused($focus, equals: .control(kind.control))
    }

    private func togglePause() {
        isScrubbing = false
        controller.togglePause()
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
        switch direction {
        case .left:
            isScrubbing = true
            controller.seek(bySteps: -1)
        case .right:
            isScrubbing = true
            controller.seek(bySteps: 1)
        default:
            break
        }
    }

    private func toggleInfo() {
        if !isInfoOpen {
            infoRows = controller.mediaInfo()
        }
        isInfoOpen.toggle()
    }

    private func choose(_ option: MpvTrackOption, of kind: MpvTrackKind) {
        controller.select(option, of: kind)
        refreshTracks()
        scheduleHide()
    }

    private func refreshTracksIfOnControl(_ newFocus: MpvFocus?) {
        guard case .control = newFocus else { return }
        refreshTracks()
    }

    private func refreshTracks() {
        subtitleOptions = controller.trackOptions(.subtitles)
        audioOptions = controller.trackOptions(.audio)
    }

    private func closeInfoIfFocusLeft(_ newFocus: MpvFocus?) {
        guard isInfoOpen, newFocus != .control(.info) else { return }
        isInfoOpen = false
    }

    private func back() {
        if isInfoOpen {
            isInfoOpen = false
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
        !controller.isPaused && !isInfoOpen && focus != nil
    }

    private func hideControls() {
        focus = .surface
        isScrubbing = false
        isControlsShown = false
    }

    private func refreshInfoWhileOpen() async {
        while isInfoOpen, !Task.isCancelled {
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
