import Foundation
import NuvioTvShared

struct MpvSubtitleDownload: Equatable {
    private static let perLanguageLimit = 4

    let url: String
    let language: String
    let languageLabel: String
    let source: String
    let number: Int

    var trackTitle: String {
        "\(languageLabel) (\(source))"
    }

    var option: MpvTrackOption {
        MpvTrackOption(
            id: "online:\(url)",
            label: number > 1 ? "\(languageLabel) \(number)" : languageLabel,
            detail: "Online from \(source)",
            isSelected: false,
            download: self
        )
    }

    static func inSourceOrder(_ subtitles: [TvosSubtitle]) -> [MpvSubtitleDownload] {
        var countByLanguage: [String: Int] = [:]
        return subtitles.compactMap { subtitle in
            let language = languagePrefix(subtitle.language)
            let number = (countByLanguage[language] ?? 0) + 1
            guard number <= perLanguageLimit else { return nil }
            countByLanguage[language] = number
            return MpvSubtitleDownload(
                url: subtitle.url,
                language: subtitle.language,
                languageLabel: subtitle.languageLabel,
                source: subtitle.source,
                number: number
            )
        }
    }

    private static func languagePrefix(_ code: String) -> String {
        String(code.lowercased().prefix { $0.isLetter })
    }
}
