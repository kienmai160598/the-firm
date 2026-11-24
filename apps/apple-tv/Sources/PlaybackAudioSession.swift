import AVFoundation
import os

/// One audio session setup for both players: a non-mixable playback session with the long-form
/// audio route sharing policy, so films play to the AirPlay speakers picked for long-form audio
/// (HomePod, AirPlay receivers) like the TV app does, not to the TV. tvOS has no long-form video
/// policy; long-form audio is the one it offers. The Info.plist sets the same policy at launch
/// (AVInitialRouteSharingPolicy).
enum PlaybackAudioSession {
    private static let log = Logger(subsystem: "com.kien.movies.tvos", category: "AudioSession")

    static func activate() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playback, mode: .moviePlayback, policy: .longFormAudio)
            try session.setActive(true)
        } catch {
            log.error("Audio session setup failed: \(error.localizedDescription, privacy: .public)")
        }
        log.notice("Audio route \(outputDescription, privacy: .public)")
    }

    /// The outputs audio plays to now, such as "Living Room (AirPlay)" or "TV (HDMI)".
    static var outputDescription: String {
        let outputs = AVAudioSession.sharedInstance().currentRoute.outputs
        guard !outputs.isEmpty else { return "None" }
        return outputs.map { "\($0.portName) (\(portLabel($0.portType)))" }.joined(separator: ", ")
    }

    private static func portLabel(_ port: AVAudioSession.Port) -> String {
        switch port {
        case .airPlay: "AirPlay"
        case .HDMI: "HDMI"
        case .bluetoothA2DP, .bluetoothLE: "Bluetooth"
        default: port.rawValue
        }
    }
}
