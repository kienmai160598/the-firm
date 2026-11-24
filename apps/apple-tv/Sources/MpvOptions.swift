import Foundation
import NuvioTvShared

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

    static func playback(
        headers: [String: String],
        languages: TvosPlayerLanguages
    ) -> [(name: String, value: String)] {
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
            ("cache", "yes"),
            // After the cache runs dry, resume only with 5 s buffered (ExoPlayer's rebuffer default).
            // mpv's default of 1 s makes a source slower than the bitrate stop and start every second.
            ("cache-pause-wait", "5"),
            ("demuxer-max-bytes", "200MiB"),
            ("demuxer-max-back-bytes", "50MiB"),
            ("network-timeout", "30"),
        ]
        options += languageOptions(languages)
        for (name, value) in headers {
            if name.lowercased() == userAgentHeader {
                options.append(("user-agent", value))
            } else {
                options.append(("http-header-fields-append", "\(name): \(value)"))
            }
        }
        return options
    }

    /// Settings' audio and subtitle languages. Containers tag tracks with two or three letter
    /// codes ("vi" or "vie"), so each language goes in both forms.
    private static func languageOptions(_ languages: TvosPlayerLanguages) -> [(name: String, value: String)] {
        var options: [(name: String, value: String)] = []
        if !languages.audio.isEmpty {
            options.append(("alang", codeList(languages.audio)))
        }
        if languages.showsSubtitles {
            options.append(("slang", codeList(languages.subtitles)))
            options.append(("subs-fallback", "yes"))
        } else {
            options.append(("sid", "no"))
        }
        return options
    }

    private static func codeList(_ codes: [String]) -> String {
        codes.flatMap { code in
            let alpha3 = Locale.LanguageCode(code).identifier(.alpha3)
            return [code, alpha3].compactMap { $0 }
        }
        .joined(separator: ",")
    }
}
