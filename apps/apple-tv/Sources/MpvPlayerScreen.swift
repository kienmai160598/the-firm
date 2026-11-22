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
        case .audio: "speaker.wave.2"
        case .info: "info.circle"
        }
    }
}

enum MpvFocus: Hashable {
    case surface
    case control(MpvControl)
    case option(String)
}

enum MpvPanel: Equatable {
    case tracks(MpvTrackKind)
    case info

    var control: MpvControl {
        switch self {
        case .tracks(.subtitles): .subtitles
        case .tracks(.audio): .audio
        case .info: .info
        }
    }
}

struct MpvPlayerScreen: View {
    private static let hideDelay: Duration = .seconds(5)
    private static let infoRefresh: Duration = .seconds(1)

    private let target: TvosStreamTarget
    private let source: String
    @StateObject private var controller: MpvPlaybackController
    @Environment(\.dismiss) private var dismiss
    @FocusState private var focus: MpvFocus?
    @State private var isControlsShown = true
    @State private var panel: MpvPanel?
    @State private var trackOptions: [MpvTrackOption] = []
    @State private var infoRows: [MpvInfoRow] = []
    @State private var hideTask: Task<Void, Never>?

    init(request: PlaybackRequest, onFailure: @escaping (StreamNotice) -> Void) {
        target = request.target
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
                controls
                    .transition(.opacity)
            }
        }
        .ignoresSafeArea()
        .animation(.easeInOut(duration: 0.25), value: isControlsShown)
        .animation(.easeInOut(duration: 0.2), value: panel)
        .onPlayPauseCommand(perform: togglePause)
        .onExitCommand(perform: back)
        .onAppear {
            controller.start()
            focus = .surface
            scheduleHide()
        }
        .onDisappear(perform: controller.finish)
        .onChange(of: focus) { _, _ in scheduleHide() }
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
        .onMoveCommand(perform: moveOnSurface)
        .accessibilityIdentifier("mpv-player")
    }

    private var controls: some View {
        ZStack(alignment: .bottom) {
            LinearGradient(
                colors: [.clear, .black.opacity(0.75)],
                startPoint: .center,
                endPoint: .bottom
            )
            .allowsHitTesting(false)
            VStack(alignment: .leading, spacing: 28) {
                panelView
                HStack(alignment: .bottom) {
                    titleBlock
                    Spacer()
                    controlButtons
                }
                MpvTransportBar(controller: controller, isFocused: focus == .surface)
                    .allowsHitTesting(false)
            }
            .padding(.horizontal, 90)
            .padding(.bottom, 60)
        }
    }

    @ViewBuilder
    private var panelView: some View {
        switch panel {
        case let .tracks(kind):
            HStack {
                Spacer()
                MpvTrackMenu(kind: kind, options: trackOptions, focus: $focus) { option in
                    choose(option, of: kind)
                }
            }
            .transition(.opacity.combined(with: .move(edge: .bottom)))
        case .info:
            MpvInfoPanel(
                title: target.title,
                subtitle: target.subtitle,
                source: source,
                rows: infoRows
            )
            .transition(.opacity.combined(with: .move(edge: .bottom)))
        case .none:
            EmptyView()
        }
    }

    private var titleBlock: some View {
        VStack(alignment: .leading, spacing: 6) {
            if let subtitle = target.subtitle {
                Text(subtitle)
                    .font(.callout.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.75))
            }
            Text(target.title)
                .font(.title2.bold())
                .lineLimit(1)
        }
        .allowsHitTesting(false)
    }

    private var controlButtons: some View {
        HStack(spacing: 22) {
            ForEach(MpvControl.allCases, id: \.self) { control in
                Button { open(control) } label: {
                    Image(systemName: control.symbol)
                        .font(.title3)
                        .frame(width: 40, height: 40)
                }
                .glassCircleButtonStyle()
                .focused($focus, equals: .control(control))
                .accessibilityLabel(control.label)
            }
        }
        .focusSection()
    }

    private func togglePause() {
        controller.togglePause()
        showControls()
    }

    private func moveOnSurface(_ direction: MoveCommandDirection) {
        switch direction {
        case .left:
            controller.seek(bySteps: -1)
        case .right:
            controller.seek(bySteps: 1)
        case .up where isControlsShown:
            focus = .control(.subtitles)
        default:
            break
        }
        showControls()
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
        DispatchQueue.main.async { focus = .option(selected.id) }
    }

    private func choose(_ option: MpvTrackOption, of kind: MpvTrackKind) {
        controller.select(option, of: kind)
        closePanel()
    }

    private func closePanel() {
        guard let closing = panel else { return }
        panel = nil
        focus = .control(closing.control)
    }

    private func back() {
        if panel != nil {
            closePanel()
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
            isControlsShown = false
        }
    }

    private var canHideControls: Bool {
        !controller.isPaused && panel == nil && focus == .surface
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
