import Foundation
import NuvioTvShared

struct MpvSubtitleDownload: Equatable {
    private static let perLanguageLimit = 4
    private static let favouriteLanguages = ["vi", "en"]

    let url: String
    let language: String
    let languageLabel: String
    let source: String
    let number: Int

    var trackTitle: String {
        "\(languageLabel) · \(source)"
    }

    var option: MpvTrackOption {
        MpvTrackOption(
            id: "online:\(url)",
            label: number > 1 ? "\(languageLabel) \(number)" : languageLabel,
            detail: "\(source) · Online",
            isSelected: false,
            download: self
        )
    }

    static func ranked(_ subtitles: [TvosSubtitle]) -> [MpvSubtitleDownload] {
        let preferred = preferredLanguages
        let byLanguage = Dictionary(grouping: subtitles) { languagePrefix($0.language) }
        return byLanguage
            .sorted { left, right in
                rank(left.key, in: preferred, label: left.value[0].languageLabel)
                    < rank(right.key, in: preferred, label: right.value[0].languageLabel)
            }
            .flatMap { _, group in
                group.prefix(perLanguageLimit).enumerated().map { index, subtitle in
                    MpvSubtitleDownload(
                        url: subtitle.url,
                        language: subtitle.language,
                        languageLabel: subtitle.languageLabel,
                        source: subtitle.source,
                        number: index + 1
                    )
                }
            }
    }

    private static var preferredLanguages: [String] {
        let device = Locale.preferredLanguages.map(languagePrefix)
        var seen = Set<String>()
        return (favouriteLanguages.prefix(1) + device + favouriteLanguages.suffix(1))
            .filter { seen.insert($0).inserted }
    }

    private static func rank(_ language: String, in preferred: [String], label: String) -> String {
        let position = preferred.firstIndex(of: language) ?? preferred.count
        return String(format: "%03d", position) + label.lowercased()
    }

    private static func languagePrefix(_ code: String) -> String {
        String(code.lowercased().prefix { $0.isLetter })
    }
}
