import MediaPlayer
import NuvioTvShared

/// Makes the app the system's Now Playing app while mpv plays. Without it the Siri Remote volume
/// buttons change the TV's volume even when the sound plays to AirPlay speakers.
/// AVPlayerViewController does the same for AVPlayer on its own.
@MainActor
final class MpvNowPlaying {
    struct Commands {
        let play: @MainActor () -> Void
        let pause: @MainActor () -> Void
        let togglePause: @MainActor () -> Void
    }

    private let baseInfo: [String: Any]
    private var registrations: [(command: MPRemoteCommand, target: Any)] = []

    init(target: TvosStreamTarget, commands: Commands) {
        var info: [String: Any] = [
            MPMediaItemPropertyTitle: target.title,
            MPNowPlayingInfoPropertyMediaType: MPNowPlayingInfoMediaType.video.rawValue,
        ]
        info[MPMediaItemPropertyArtist] = target.subtitle
        baseInfo = info
        let center = MPRemoteCommandCenter.shared()
        register(center.playCommand, commands.play)
        register(center.pauseCommand, commands.pause)
        register(center.togglePlayPauseCommand, commands.togglePause)
    }

    /// The system runs the elapsed time on from here at the given rate, so this is needed only when
    /// the position jumps or the rate changes.
    func update(positionSeconds: Double, durationSeconds: Double, isMoving: Bool) {
        var info = baseInfo
        info[MPMediaItemPropertyPlaybackDuration] = durationSeconds
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = positionSeconds
        info[MPNowPlayingInfoPropertyPlaybackRate] = isMoving ? 1.0 : 0.0
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    func clear() {
        registrations.forEach { $0.command.removeTarget($0.target) }
        registrations = []
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
    }

    private func register(_ command: MPRemoteCommand, _ action: @escaping @MainActor () -> Void) {
        command.isEnabled = true
        let target = command.addTarget { _ in
            Task { @MainActor in action() }
            return .success
        }
        registrations.append((command, target))
    }
}
