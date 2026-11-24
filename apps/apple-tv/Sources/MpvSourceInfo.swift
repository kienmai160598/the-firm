import NuvioTvShared

/// What the Info panel says about the stream that plays: its name, the tags the Streams row shows
/// (resolution, HDR, audio, codec), and one line with the addon, the size and the delivery.
struct MpvSourceInfo: Equatable {
    private static let detailSeparator = " · "

    let title: String
    let badges: [String]
    let detail: String

    init(stream: TvosStream) {
        let row = StreamRowData(stream: stream)
        title = row.title
        badges = (row.resolution.map { [$0] } ?? []) + row.capabilities
        let delivery = row.isTorrent ? "Torrent" : "Direct"
        detail = [stream.addonName, row.size, delivery]
            .compactMap { $0 }
            .joined(separator: Self.detailSeparator)
    }
}
