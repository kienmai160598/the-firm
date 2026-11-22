import Foundation

struct StreamFeatures: Equatable {
    let resolution: String?
    let size: String?
    let seeders: Int?
    let capabilities: [String]

    init(texts: [String], providedBadges: [String], providedSize: String?) {
        let haystack = texts.joined(separator: " \n ")
        let foundResolution = Self.firstMatch(in: haystack, rules: Self.resolutionRules)
        resolution = foundResolution
        size = providedSize ?? Self.size(in: haystack)
        seeders = Self.seeders(in: haystack)
        let found = [
            Self.dynamicRanges(in: haystack),
            Self.firstMatch(in: haystack, rules: Self.audioRules).map { [$0] } ?? [],
            Self.firstMatch(in: haystack, rules: Self.channelRules).map { [$0] } ?? [],
            Self.firstMatch(in: haystack, rules: Self.codecRules).map { [$0] } ?? [],
            Self.firstMatch(in: haystack, rules: Self.bitDepthRules).map { [$0] } ?? [],
            Self.firstMatch(in: haystack, rules: Self.sourceRules).map { [$0] } ?? [],
        ].flatMap { $0 }
        capabilities = Self.unique(found + providedBadges.filter { $0 != foundResolution })
    }

    private typealias Rule = (pattern: String, label: String)

    private static let resolutionRules: [Rule] = [
        (#"\b(2160p|4k|uhd)\b"#, "4K"),
        (#"\b1440p\b"#, "1440p"),
        (#"\b(1080p|fhd)\b"#, "1080p"),
        (#"\b720p\b"#, "720p"),
        (#"\b(576p|480p|360p)\b"#, "SD"),
    ]

    private static let dynamicRangeRules: [Rule] = [
        (#"\b(dolby ?vision|dovi|dv)\b"#, "Dolby Vision"),
        (#"\bhdr10(\+|plus)"#, "HDR10+"),
        (#"\bhdr10\b(?!\+)"#, "HDR10"),
        (#"\bhdr\b(?!10)"#, "HDR"),
        (#"\bhlg\b"#, "HLG"),
    ]

    private static let audioRules: [Rule] = [
        (#"\batmos\b"#, "Dolby Atmos"),
        (#"\btrue ?hd\b"#, "TrueHD"),
        (#"\bdts[ -:]?x\b"#, "DTS:X"),
        (#"\bdts[ -]?hd"#, "DTS-HD"),
        (#"\bdts\b"#, "DTS"),
        (#"\b(ddp|dd\+|e-?ac-?3|dolby digital plus)"#, "Dolby Digital+"),
        (#"\b(dd|ac-?3|dolby digital)\b"#, "Dolby Digital"),
        (#"\bflac\b"#, "FLAC"),
        (#"\bopus\b"#, "Opus"),
        (#"\baac\b"#, "AAC"),
    ]

    private static let channelRules: [Rule] = [
        (#"(?<!\d)7[ .]1(?!\d|\s?[KMGT]i?B)"#, "7.1"),
        (#"(?<!\d)5[ .]1(?!\d|\s?[KMGT]i?B)"#, "5.1"),
        (#"(?<!\d)2[ .]0(?!\d|\s?[KMGT]i?B)"#, "Stereo"),
    ]

    private static let codecRules: [Rule] = [
        (#"\b(hevc|x265|h\.?265)\b"#, "HEVC"),
        (#"\bav1\b"#, "AV1"),
        (#"\b(avc|x264|h\.?264)\b"#, "H.264"),
    ]

    private static let bitDepthRules: [Rule] = [
        (#"\b10[ -]?bit\b"#, "10-bit"),
    ]

    private static let sourceRules: [Rule] = [
        (#"\bremux\b"#, "REMUX"),
        (#"\b(blu-?ray|bdrip|brrip)\b"#, "Blu-ray"),
        (#"\bweb-?dl\b"#, "WEB-DL"),
        (#"\bweb-?rip\b"#, "WEBRip"),
        (#"\bhdtv\b"#, "HDTV"),
        (#"\b(cam|hdcam|telesync)\b"#, "CAM"),
    ]

    private static let sizePattern = #"(\d+(?:[.,]\d+)?)\s?(TB|GB|MB|TiB|GiB|MiB)\b"#
    private static let seedersPattern = #"(?:👤|seeders?:?|seeds?:?)\s?(\d+)"#

    private static func dynamicRanges(in text: String) -> [String] {
        let found = allMatches(in: text, rules: dynamicRangeRules)
        let hasNamedHdr = found.contains { $0.hasPrefix("HDR10") }
        return hasNamedHdr ? found.filter { $0 != "HDR" } : found
    }

    private static func firstMatch(in text: String, rules: [Rule]) -> String? {
        rules.first { matches(text, $0.pattern) }?.label
    }

    private static func allMatches(in text: String, rules: [Rule]) -> [String] {
        rules.filter { matches(text, $0.pattern) }.map(\.label)
    }

    private static func matches(_ text: String, _ pattern: String) -> Bool {
        text.range(of: pattern, options: [.regularExpression, .caseInsensitive]) != nil
    }

    private static func size(in text: String) -> String? {
        guard let match = captures(in: text, pattern: sizePattern), match.count == 2 else {
            return nil
        }
        let amount = match[0].replacingOccurrences(of: ",", with: ".")
        let unit = match[1].uppercased().replacingOccurrences(of: "I", with: "")
        return "\(amount) \(unit)"
    }

    private static func seeders(in text: String) -> Int? {
        captures(in: text, pattern: seedersPattern)?.first.flatMap(Int.init)
    }

    private static func captures(in text: String, pattern: String) -> [String]? {
        guard let expression = try? NSRegularExpression(
            pattern: pattern,
            options: [.caseInsensitive]
        ),
            let match = expression.firstMatch(
                in: text,
                range: NSRange(text.startIndex..., in: text)
            )
        else { return nil }
        return (1..<match.numberOfRanges).compactMap { index in
            Range(match.range(at: index), in: text).map { String(text[$0]) }
        }
    }

    private static func unique(_ labels: [String]) -> [String] {
        var seen = Set<String>()
        return labels.filter { seen.insert($0.lowercased()).inserted }
    }
}
