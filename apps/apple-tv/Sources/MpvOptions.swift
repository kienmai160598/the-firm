import Foundation

enum MpvProperty {
    static let timePosition = "time-pos"
    static let duration = "duration"
    static let pause = "pause"
    static let pausedForCache = "paused-for-cache"
    static let video = "vid"
    static let endReached = "eof-reached"
    static let cacheTime = "demuxer-cache-time"
    static let subtitlePosition = "sub-pos"
    static let colorspaceHint = "target-colorspace-hint"
}

enum MpvOptions {
    private static let userAgentHeader = "user-agent"
    private static let subtitleLanguages = "vi,vie,en,eng"

    static func playback(headers: [String: String]) -> [(name: String, value: String)] {
        var options: [(name: String, value: String)] = [
            ("vo", "gpu-next"),
            ("gpu-api", "vulkan"),
            ("gpu-context", "moltenvk"),
            ("hwdec", "videotoolbox"),
            // AVSampleBufferAudioRenderer, the renderer AVPlayer uses, reaches AirPlay speakers; the
            // default RemoteIO audio unit opens a mixable real-time session instead.
            ("ao", "avfoundation,audiounit"),
            ("video-rotate", "no"),
            ("pause", "yes"),
            ("keep-open", "yes"),
            ("slang", subtitleLanguages),
            ("subs-fallback", "yes"),
            ("cache", "yes"),
            // After the cache runs dry, resume only with 5 s buffered (ExoPlayer's rebuffer default).
            // mpv's default of 1 s makes a source slower than the bitrate stop and start every second.
            ("cache-pause-wait", "5"),
            ("demuxer-max-bytes", "200MiB"),
            ("demuxer-max-back-bytes", "50MiB"),
            ("network-timeout", "30"),
        ]
        for (name, value) in headers {
            if name.lowercased() == userAgentHeader {
                options.append(("user-agent", value))
            } else {
                options.append(("http-header-fields-append", "\(name): \(value)"))
            }
        }
        return options
    }
}
