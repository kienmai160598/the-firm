import AVFoundation
import MediaPlayer

/// Makes a trailer the system's Now Playing item while it plays, so the Siri Remote volume
/// buttons change the AirPlay speakers it plays to, not the TV, as MpvNowPlaying does for films.
/// The session publishes the player's time and rate by itself; releasing this object ends it.
@MainActor
final class TrailerNowPlaying {
    private let session: MPNowPlayingSession

    init(player: AVPlayer) {
        session = MPNowPlayingSession(players: [player])
        session.automaticallyPublishesNowPlayingInfo = true
        let commands = session.remoteCommandCenter
        Self.handle(commands.playCommand) { player.play() }
        Self.handle(commands.pauseCommand) { player.pause() }
        Self.handle(commands.togglePlayPauseCommand) {
            player.timeControlStatus == .paused ? player.play() : player.pause()
        }
        session.becomeActiveIfPossible()
    }

    private static func handle(_ command: MPRemoteCommand, _ action: @escaping @MainActor () -> Void) {
        command.isEnabled = true
        command.addTarget { _ in
            Task { @MainActor in action() }
            return .success
        }
    }
}
