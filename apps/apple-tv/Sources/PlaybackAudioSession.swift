import AVFoundation
import os

/// One audio session setup for both players: a non-mixable playback session, so tvOS treats the
/// app as the main audio and sends it to the output the viewer picked, AirPlay speakers included.
/// tvOS has no long-form route sharing policy; routing always follows the system output there.
enum PlaybackAudioSession {
    private static let log = Logger(subsystem: "com.kien.movies.tvos", category: "AudioSession")

    static func activate() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playback, mode: .moviePlayback, options: [])
            try session.setActive(true)
        } catch {
            log.error("Audio session setup failed: \(error.localizedDescription, privacy: .public)")
        }
    }
}
