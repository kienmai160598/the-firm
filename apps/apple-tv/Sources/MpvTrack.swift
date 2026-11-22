import Foundation

struct MpvTrackOption: Identifiable, Equatable {
    let id: String
    let label: String
    let detail: String?
    let isSelected: Bool
}

enum MpvTrackKind {
    case audio
    case subtitles

    private static let offId = "no"

    var title: String {
        switch self {
        case .audio: "Audio"
        case .subtitles: "Subtitles"
        }
    }

    var selector: String {
        switch self {
        case .audio: "aid"
        case .subtitles: "sid"
        }
    }

    private var trackType: String {
        switch self {
        case .audio: "audio"
        case .subtitles: "sub"
        }
    }

    func options(using mpv: MpvHandle) -> [MpvTrackOption] {
        let count = mpv.string("track-list/count").flatMap(Int.init) ?? 0
        let tracks = (0..<count).compactMap { index in option(at: index, using: mpv) }
        guard self == .subtitles else { return tracks }
        let isOff = !tracks.contains(where: \.isSelected)
        return [MpvTrackOption(id: Self.offId, label: "Off", detail: nil, isSelected: isOff)]
            + tracks
    }

    private func option(at index: Int, using mpv: MpvHandle) -> MpvTrackOption? {
        let prefix = "track-list/\(index)/"
        guard mpv.string(prefix + "type") == trackType,
              let id = mpv.string(prefix + "id")
        else { return nil }
        let title = mpv.string(prefix + "title").flatMap(Self.nonEmpty)
        let language = mpv.string(prefix + "lang").flatMap(Self.languageName)
        let codec = mpv.string(prefix + "codec").map { $0.uppercased() }
        let channels = mpv.string(prefix + "demux-channel-count").flatMap(Int.init)
            .map(Self.channelLabel)
        let label = title ?? language ?? "Track \(id)"
        let details = [title == nil ? nil : language, codec, channels].compactMap { $0 }
        return MpvTrackOption(
            id: id,
            label: label,
            detail: details.isEmpty ? nil : details.joined(separator: " · "),
            isSelected: mpv.string(prefix + "selected") == "yes"
        )
    }

    private static func nonEmpty(_ text: String) -> String? {
        text.isEmpty ? nil : text
    }

    private static func languageName(_ code: String) -> String? {
        guard !code.isEmpty, code != "und" else { return nil }
        return Locale.current.localizedString(forLanguageCode: code)?.capitalized ?? code
    }

    static func channelLabel(_ count: Int) -> String {
        switch count {
        case 1: "Mono"
        case 2: "Stereo"
        case 6: "5.1"
        case 8: "7.1"
        default: "\(count) ch"
        }
    }
}
