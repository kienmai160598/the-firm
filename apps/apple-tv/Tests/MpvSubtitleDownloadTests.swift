import NuvioTvShared
import XCTest
@testable import NuvioTv

final class MpvSubtitleDownloadTests: XCTestCase {
    func testVietnameseComesFirstWithAtMostFourPerLanguage() {
        let subtitles = [subtitle("fr", "French", 1), subtitle("en", "English", 1)]
            + (1...6).map { subtitle("vi", "Vietnamese", $0) }

        let ranked = MpvSubtitleDownload.ranked(subtitles)

        XCTAssertEqual(
            ranked.map(\.option.label),
            ["Vietnamese", "Vietnamese 2", "Vietnamese 3", "Vietnamese 4", "English", "French"]
        )
        XCTAssertEqual(ranked[0].url, "https://subs.test/vi/1.srt")
        XCTAssertEqual(ranked[0].trackTitle, "Vietnamese · OpenSubtitles")
    }

    private func subtitle(_ language: String, _ label: String, _ number: Int) -> TvosSubtitle {
        TvosSubtitle(
            url: "https://subs.test/\(language)/\(number).srt",
            language: language,
            languageLabel: label,
            source: "OpenSubtitles"
        )
    }
}
