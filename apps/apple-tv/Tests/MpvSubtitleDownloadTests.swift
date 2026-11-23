import NuvioTvShared
import XCTest
@testable import NuvioTv

final class MpvSubtitleDownloadTests: XCTestCase {
    func testKeepsSourceOrderWithAtMostFourPerLanguage() {
        let subtitles = [subtitle("fr", "French", 1), subtitle("en", "English", 1)]
            + (1...6).map { subtitle("vi", "Vietnamese", $0) }

        let downloads = MpvSubtitleDownload.inSourceOrder(subtitles)

        XCTAssertEqual(
            downloads.map(\.option.label),
            ["French", "English", "Vietnamese", "Vietnamese 2", "Vietnamese 3", "Vietnamese 4"]
        )
        XCTAssertEqual(downloads[2].url, "https://subs.test/vi/1.srt")
        XCTAssertEqual(downloads[2].trackTitle, "Vietnamese (OpenSubtitles)")
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
