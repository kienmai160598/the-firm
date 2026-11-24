import NuvioTvShared

/// What the Info panel says about the stream that plays: its name, the addon it came from, the
/// file, and the facts the Streams row shows (resolution, HDR and codecs, size, torrent or link).
struct MpvSourceInfo: Equatable {
    private static let factSeparator = " · "

    let title: String
    let addon: String
    let file: String?
    let facts: String

    init(stream: TvosStream) {
        let row = StreamRowData(stream: stream)
        title = row.title
        addon = stream.addonName
        file = row.fileLine
        let delivery = row.isTorrent ? Self.torrentFact(seeders: row.seeders) : "Direct link"
        let facts: [String?] =
            [row.resolution] + row.capabilities.map(Optional.some) + [row.size, delivery]
        self.facts = facts.compactMap { $0 }.joined(separator: Self.factSeparator)
    }

    private static func torrentFact(seeders: Int?) -> String {
        guard let seeders else { return "Torrent" }
        return "Torrent, \(seeders) seeders"
    }
}
