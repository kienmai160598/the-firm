import Foundation

struct MpvTrack {
    static let audio = MpvTrack(selector: "aid", kind: "audio", label: "Audio")
    static let subtitles = MpvTrack(selector: "sid", kind: "sub", label: "Subtitles")

    let selector: String
    let kind: String
    let label: String

    func describe(using mpv: MpvHandle) -> String {
        guard let selected = mpv.string(selector), selected != "no" else {
            return "\(label): off"
        }
        let details = ["title", "lang", "codec"]
            .compactMap { mpv.string("current-tracks/\(kind)/\($0)") }
            .filter { !$0.isEmpty }
        let name = details.isEmpty ? "track \(selected)" : details.joined(separator: " · ")
        return "\(label): \(name)"
    }
}
