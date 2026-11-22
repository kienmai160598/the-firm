import SwiftUI

struct MpvPlayerScreen: View {
    private static let overlayDuration: Duration = .seconds(4)

    let title: String
    @StateObject private var controller: MpvPlaybackController
    @Environment(\.dismiss) private var dismiss
    @State private var overlayTask: Task<Void, Never>?
    @State private var isOverlayShown = true

    init(request: PlaybackRequest, onFailure: @escaping (StreamNotice) -> Void) {
        title = request.target.title
        _controller = StateObject(
            wrappedValue: MpvPlaybackController(request: request, onFailure: onFailure)
        )
    }

    var body: some View {
        ZStack {
            Button(action: controller.togglePause) {
                MpvVideoView(videoLayer: controller.videoLayer)
            }
            .buttonStyle(MpvSurfaceButtonStyle())
            .accessibilityIdentifier("mpv-player")
            MpvOverlay(
                title: title,
                controller: controller,
                isControlsShown: isOverlayShown || controller.isPaused
            )
            .allowsHitTesting(false)
        }
        .ignoresSafeArea()
        .onPlayPauseCommand(perform: controller.togglePause)
        .onMoveCommand(perform: move)
        .onExitCommand { dismiss() }
        .onAppear {
            controller.start()
            showOverlay()
        }
        .onDisappear(perform: controller.finish)
    }

    private func move(_ direction: MoveCommandDirection) {
        switch direction {
        case .left:
            controller.seek(bySteps: -1)
        case .right:
            controller.seek(bySteps: 1)
        case .up:
            controller.cycleAudio()
        case .down:
            controller.cycleSubtitles()
        @unknown default:
            break
        }
        showOverlay()
    }

    private func showOverlay() {
        isOverlayShown = true
        overlayTask?.cancel()
        overlayTask = Task { @MainActor in
            try? await Task.sleep(for: Self.overlayDuration)
            guard !Task.isCancelled else { return }
            withAnimation { isOverlayShown = false }
        }
    }
}

private struct MpvSurfaceButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
    }
}

private struct MpvOverlay: View {
    let title: String
    @ObservedObject var controller: MpvPlaybackController
    let isControlsShown: Bool

    var body: some View {
        ZStack {
            if controller.isBuffering {
                ProgressView()
                    .scaleEffect(1.6)
            }
            VStack {
                HStack {
                    Spacer()
                    if let message = controller.trackMessage {
                        MpvToast(message: message)
                    }
                }
                Spacer()
                if isControlsShown {
                    MpvControls(title: title, controller: controller)
                        .transition(.opacity)
                }
            }
            .padding(.horizontal, 90)
            .padding(.vertical, 60)
        }
        .animation(.easeInOut(duration: 0.2), value: isControlsShown)
    }
}

private struct MpvToast: View {
    let message: String

    var body: some View {
        Text(message)
            .font(.headline)
            .padding(.horizontal, 28)
            .padding(.vertical, 16)
            .background(.black.opacity(0.7), in: Capsule())
    }
}

private struct MpvControls: View {
    private static let hint = "◀ ▶ 10 s    ▲ Audio    ▼ Subtitles"

    let title: String
    @ObservedObject var controller: MpvPlaybackController

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(alignment: .firstTextBaseline) {
                Image(systemName: controller.isPaused ? "pause.fill" : "play.fill")
                Text(title)
                    .font(.title3.bold())
                    .lineLimit(1)
                Spacer()
                Text(Self.hint)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            ProgressView(value: progress)
                .tint(.white)
            HStack {
                Text(MpvClock.label(controller.positionSeconds))
                Spacer()
                Text(MpvClock.label(controller.durationSeconds))
            }
            .font(.caption.monospacedDigit())
            .foregroundStyle(.secondary)
        }
        .padding(32)
        .background(.black.opacity(0.65), in: RoundedRectangle(cornerRadius: 24))
    }

    private var progress: Double {
        guard controller.durationSeconds > 0 else { return 0 }
        return min(Double(controller.positionSeconds) / Double(controller.durationSeconds), 1)
    }
}

private enum MpvClock {
    static func label(_ totalSeconds: Int) -> String {
        let hours = totalSeconds / 3600
        let minutes = totalSeconds / 60 % 60
        let seconds = totalSeconds % 60
        guard hours > 0 else { return String(format: "%d:%02d", minutes, seconds) }
        return String(format: "%d:%02d:%02d", hours, minutes, seconds)
    }
}
