import Foundation

struct MpvInfoRow: Identifiable, Equatable {
    let label: String
    let value: String

    var id: String { label }
}

enum MpvMediaInfo {
    static func rows(using mpv: MpvHandle) -> [MpvInfoRow] {
        [
            MpvInfoRow(label: "Video", value: video(using: mpv)),
            MpvInfoRow(label: "Audio", value: audio(using: mpv)),
            MpvInfoRow(label: "Decoder", value: decoder(using: mpv)),
            MpvInfoRow(label: "Buffered", value: buffered(using: mpv)),
        ]
    }

    private static func video(using mpv: MpvHandle) -> String {
        let size = mpv.string("video-params/w").flatMap { width in
            mpv.string("video-params/h").map { height in "\(width)×\(height)" }
        }
        let parts = [
            size,
            mpv.string("video-format")?.uppercased(),
            bitDepth(mpv.string("video-params/hw-pixelformat"))
                ?? bitDepth(mpv.string("video-params/pixelformat")),
            dynamicRange(mpv.string("video-params/gamma")),
        ]
        return joined(parts)
    }

    private static func audio(using mpv: MpvHandle) -> String {
        let channels = mpv.string("audio-params/channel-count").flatMap(Int.init)
            .map(MpvTrackKind.channelLabel)
        return joined([mpv.string("audio-codec-name")?.uppercased(), channels])
    }

    private static func decoder(using mpv: MpvHandle) -> String {
        switch mpv.string("hwdec-current") {
        case let .some(name) where !name.isEmpty && name != "no":
            return "Hardware (\(name == "videotoolbox" ? "VideoToolbox" : name))"
        default:
            return "Software"
        }
    }

    private static func buffered(using mpv: MpvHandle) -> String {
        guard let seconds = mpv.number("demuxer-cache-duration"), seconds.isFinite else {
            return "None"
        }
        return "\(Int(seconds)) s ahead"
    }

    private static func bitDepth(_ pixelFormat: String?) -> String? {
        guard let pixelFormat else { return nil }
        if pixelFormat.contains("p10") || pixelFormat.contains("p010") { return "10-bit" }
        if pixelFormat.contains("p12") || pixelFormat.contains("p012") { return "12-bit" }
        return nil
    }

    private static func dynamicRange(_ gamma: String?) -> String? {
        switch gamma {
        case "pq": "HDR10"
        case "hlg": "HLG"
        default: nil
        }
    }

    private static func joined(_ parts: [String?]) -> String {
        let present = parts.compactMap { $0 }.filter { !$0.isEmpty }
        return present.isEmpty ? "Unknown" : present.joined(separator: ", ")
    }
}
