import NuvioTvShared

extension TvosStream: Identifiable {
    public var id: String { key }
}

/// What a stream row shows, read once per stream instead of on every render.
struct StreamRowData: Identifiable, Equatable {
    private static let maxCapabilities = 4

    let stream: TvosStream
    let title: String
    let resolution: String?
    let capabilities: [String]
    /// The capabilities, then the file format (MKV, MP4, HLS), as the row's tag line shows them.
    let tags: [String]
    let size: String?
    let seeders: Int?

    var id: String { stream.key }
    var isTorrent: Bool { stream.infoHash != nil }

    init(stream: TvosStream) {
        let features = StreamFeatures(
            texts: [stream.name, stream.title, stream.detail, stream.filename].compactMap { $0 },
            providedBadges: stream.badges,
            providedSize: stream.sizeLabel
        )
        self.stream = stream
        title = Self.oneLine(stream.name)
        resolution = features.resolution
        capabilities = Array(features.capabilities.prefix(Self.maxCapabilities))
        tags = capabilities + [StreamFileFormat.shared.of(stream: stream)].compactMap { $0 }
        size = features.size
        seeders = features.seeders
    }

    /// Rows built from the same stream instance show the same thing.
    static func == (lhs: StreamRowData, rhs: StreamRowData) -> Bool {
        lhs.stream === rhs.stream
    }

    private static func oneLine(_ name: String) -> String {
        name.split(whereSeparator: \.isNewline)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
            .joined(separator: "   ")
    }
}

/// One line of the Streams list: an addon heading or a stream row.
enum StreamListItem: Identifiable, Equatable {
    case addon(id: String, name: String, errorMessage: String?, isFirst: Bool)
    case stream(StreamRowData)

    var id: String {
        switch self {
        case let .addon(id, _, _, _): "addon:" + id
        case let .stream(row): "stream:" + row.id
        }
    }
}

/// Turns stream groups into list items and keeps each row's parsed data while its stream is
/// unchanged, so a new addon result parses only the new streams.
struct StreamListBuilder {
    private var rows: [String: StreamRowData] = [:]

    mutating func items(for groups: [TvosStreamGroup]) -> [StreamListItem] {
        var kept: [String: StreamRowData] = [:]
        var items: [StreamListItem] = []
        for group in groups where !group.streams.isEmpty || group.errorMessage != nil {
            let error = group.streams.isEmpty ? group.errorMessage : nil
            items.append(.addon(id: group.addonId, name: group.addonName, errorMessage: error,
                                isFirst: items.isEmpty))
            for stream in group.streams {
                let row = row(for: stream)
                kept[row.id] = row
                items.append(.stream(row))
            }
        }
        rows = kept
        return items
    }

    private func row(for stream: TvosStream) -> StreamRowData {
        if let cached = rows[stream.key], cached.stream == stream { return cached }
        return StreamRowData(stream: stream)
    }
}
