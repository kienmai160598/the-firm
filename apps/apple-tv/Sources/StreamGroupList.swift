import SwiftUI
import NuvioTvShared

/// Every addon's streams in one lazy list, so only the rows on screen are built. A row redraws
/// only when its own stream, its checking state or its dimming changes.
struct StreamGroupList: View {
    private static let rowSpacing: CGFloat = 14
    private static let addonSpacing: CGFloat = 30
    private static let focusRoom: CGFloat = 24

    let items: [StreamListItem]
    let checkingStreamKey: String?
    let torrent: TorrentSession
    let onSelect: (TvosStream) -> Void

    var body: some View {
        ScrollView(.vertical) {
            LazyVStack(alignment: .leading, spacing: Self.rowSpacing) {
                ForEach(items) { item in
                    switch item {
                    case let .addon(_, name, errorMessage, isFirst):
                        StreamAddonHeading(name: name, errorMessage: errorMessage)
                            .padding(.top, isFirst ? 0 : Self.addonSpacing)
                    case let .stream(row):
                        StreamButton(
                            row: row,
                            isChecking: checkingStreamKey == row.id,
                            isDimmed: checkingStreamKey.map { $0 != row.id } ?? false,
                            torrent: torrent,
                            onSelect: onSelect
                        )
                        .equatable()
                    }
                }
            }
            .padding(Self.focusRoom)
        }
        .padding(-Self.focusRoom)
        .scrollClipDisabled()
        .focusSection()
    }
}

private struct StreamAddonHeading: View {
    let name: String
    let errorMessage: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(name)
                .font(.system(size: 27, weight: .semibold))
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .accessibilityAddTraits(.isHeader)
            if let errorMessage {
                Label(errorMessage, systemImage: "exclamationmark.triangle")
                    .font(.system(size: 25))
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.leading, 8)
        .padding(.bottom, 4)
    }
}

private struct StreamButton: View, Equatable {
    private static let dimmedOpacity = 0.45

    let row: StreamRowData
    let isChecking: Bool
    let isDimmed: Bool
    let torrent: TorrentSession
    let onSelect: (TvosStream) -> Void
    @Environment(\.accessibilityReduceMotion) private var reducesMotion

    var body: some View {
        Button {
            onSelect(row.stream)
        } label: {
            StreamRow(row: row, isChecking: isChecking, torrent: torrent)
        }
        .buttonStyle(.bordered)
        .buttonBorderShape(.roundedRectangle(radius: StreamRow.buttonCornerRadius))
        .opacity(isDimmed ? Self.dimmedOpacity : 1)
        .animation(StreamsMotion.animation(reducesMotion: reducesMotion), value: isChecking)
        .animation(StreamsMotion.animation(reducesMotion: reducesMotion), value: isDimmed)
    }

    /// The select action is the same for every render, so it takes no part in the comparison.
    static func == (lhs: StreamButton, rhs: StreamButton) -> Bool {
        lhs.row == rhs.row && lhs.isChecking == rhs.isChecking && lhs.isDimmed == rhs.isDimmed
            && lhs.torrent === rhs.torrent
    }
}
