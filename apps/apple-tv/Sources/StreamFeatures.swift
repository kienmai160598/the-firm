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

    private struct Rule {
        let expression: NSRegularExpression
        let label: String

        init(_ pattern: String, _ label: String) {
            expression = StreamFeatures.compile(pattern)
            self.label = label
        }
    }

    private static let resolutionRules: [Rule] = [
        Rule(#"\b(2160p|4k|uhd)\b"#, "4K"),
        Rule(#"\b1440p\b"#, "1440p"),
        Rule(#"\b(1080p|fhd)\b"#, "1080p"),
        Rule(#"\b720p\b"#, "720p"),
        Rule(#"\b(576p|480p|360p)\b"#, "SD"),
    ]

    private static let dynamicRangeRules: [Rule] = [
        Rule(#"\b(dolby ?vision|dovi|dv)\b"#, "Dolby Vision"),
        Rule(#"\bhdr10(\+|plus)"#, "HDR10+"),
        Rule(#"\bhdr10\b(?!\+)"#, "HDR10"),
        Rule(#"\bhdr\b(?!10)"#, "HDR"),
        Rule(#"\bhlg\b"#, "HLG"),
    ]

    private static let audioRules: [Rule] = [
        Rule(#"\batmos\b"#, "Dolby Atmos"),
        Rule(#"\btrue ?hd\b"#, "TrueHD"),
        Rule(#"\bdts[ -:]?x\b"#, "DTS:X"),
        Rule(#"\bdts[ -]?hd"#, "DTS-HD"),
        Rule(#"\bdts\b"#, "DTS"),
        Rule(#"\b(ddp|dd\+|e-?ac-?3|dolby digital plus)"#, "Dolby Digital+"),
        Rule(#"\b(dd|ac-?3|dolby digital)\b"#, "Dolby Digital"),
        Rule(#"\bflac\b"#, "FLAC"),
        Rule(#"\bopus\b"#, "Opus"),
        Rule(#"\baac\b"#, "AAC"),
    ]

    private static let channelRules: [Rule] = [
        Rule(#"(?<!\d)7[ .]1(?!\d|\s?[KMGT]i?B)"#, "7.1"),
        Rule(#"(?<!\d)5[ .]1(?!\d|\s?[KMGT]i?B)"#, "5.1"),
        Rule(#"(?<!\d)2[ .]0(?!\d|\s?[KMGT]i?B)"#, "Stereo"),
    ]

    private static let codecRules: [Rule] = [
        Rule(#"\b(hevc|x265|h\.?265)\b"#, "HEVC"),
        Rule(#"\bav1\b"#, "AV1"),
        Rule(#"\b(avc|x264|h\.?264)\b"#, "H.264"),
    ]

    private static let bitDepthRules: [Rule] = [
        Rule(#"\b10[ -]?bit\b"#, "10-bit"),
    ]

    private static let sourceRules: [Rule] = [
        Rule(#"\bremux\b"#, "REMUX"),
        Rule(#"\b(blu-?ray|bdrip|brrip)\b"#, "Blu-ray"),
        Rule(#"\bweb-?dl\b"#, "WEB-DL"),
        Rule(#"\bweb-?rip\b"#, "WEBRip"),
        Rule(#"\bhdtv\b"#, "HDTV"),
        Rule(#"\b(cam|hdcam|telesync)\b"#, "CAM"),
    ]

    private static let sizePattern = compile(#"(\d+(?:[.,]\d+)?)\s?(TB|GB|MB|TiB|GiB|MiB)\b"#)
    private static let seedersPattern = compile(#"(?:👤|seeders?:?|seeds?:?)\s?(\d+)"#)

    /// Patterns are constants checked by the tests, so a bad one is a programming error.
    private static func compile(_ pattern: String) -> NSRegularExpression {
        do {
            return try NSRegularExpression(pattern: pattern, options: [.caseInsensitive])
        } catch {
            preconditionFailure("Invalid stream feature pattern \(pattern): \(error)")
        }
    }

    private static func dynamicRanges(in text: String) -> [String] {
        let found = allMatches(in: text, rules: dynamicRangeRules)
        let hasNamedHdr = found.contains { $0.hasPrefix("HDR10") }
        return hasNamedHdr ? found.filter { $0 != "HDR" } : found
    }

    private static func firstMatch(in text: String, rules: [Rule]) -> String? {
        rules.first { matches(text, $0.expression) }?.label
    }

    private static func allMatches(in text: String, rules: [Rule]) -> [String] {
        rules.filter { matches(text, $0.expression) }.map(\.label)
    }

    private static func matches(_ text: String, _ expression: NSRegularExpression) -> Bool {
        expression.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)) != nil
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

    private static func captures(in text: String, pattern expression: NSRegularExpression) -> [String]? {
        guard let match = expression.firstMatch(in: text, range: NSRange(text.startIndex..., in: text))
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
