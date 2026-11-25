import SwiftUI

struct StreamRow: View {
    private static let height: CGFloat = 104
    /// Vertical inset of the `.bordered` button around its label, measured on the capture (134 pt
    /// button around the 104 pt row).
    private static let buttonInset: CGFloat = 15
    /// The `.bordered` button's side inset (30 pt, measured like `buttonInset`) plus the row's own 8 pt: where the row's badge
    /// starts. The addon heading above the rows starts there too, so the button hangs out past it.
    static let contentLeading: CGFloat = borderedSideInset + horizontalPadding
    private static let borderedSideInset: CGFloat = 30
    private static let horizontalPadding: CGFloat = 8

    /// The stream button's corner by the concentric rule, outer radius = inner radius + padding:
    /// the badge's 14 pt plus its inset from the button's edge, 15 + (104 - 64) / 2 = 35 pt.
    static let buttonCornerRadius = StreamResolutionBadge.cornerRadius
        + buttonInset + (height - StreamResolutionBadge.size.height) / 2

    let row: StreamRowData
    let isChecking: Bool
    let torrent: TorrentSession

    var body: some View {
        HStack(spacing: 28) {
            StreamResolutionBadge(resolution: row.resolution)
            VStack(alignment: .leading, spacing: 12) {
                Text(row.title)
                    .font(.system(size: 29, weight: .semibold))
                    .lineLimit(1)
                StreamDetailLine(capabilities: row.capabilities, fileLine: row.fileLine)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            trailing
        }
        .padding(.horizontal, Self.horizontalPadding)
        .frame(height: Self.height)
    }

    @ViewBuilder
    private var trailing: some View {
        if isChecking {
            StreamCheckingStatus(streamKey: row.id, torrent: torrent)
                .transition(.opacity)
        } else {
            StreamFacts(size: row.size, seeders: row.seeders, isTorrent: row.isTorrent)
                .transition(.opacity)
        }
    }
}

private struct StreamResolutionBadge: View {
    static let size = CGSize(width: 124, height: 64)
    static let cornerRadius: CGFloat = 14

    let resolution: String?

    var body: some View {
        Group {
            if let resolution {
                Text(resolution)
                    .font(.system(size: 28, weight: .bold))
                    .minimumScaleFactor(0.7)
            } else {
                Image(systemName: "play.rectangle")
                    .font(.system(size: 30, weight: .medium))
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)
            }
        }
        .lineLimit(1)
        .frame(width: Self.size.width, height: Self.size.height)
        .background(
            RoundedRectangle(cornerRadius: Self.cornerRadius, style: .continuous)
                .fill(Color.primary.opacity(0.1))
        )
    }
}

private struct StreamDetailLine: View {
    let capabilities: [String]
    let fileLine: String?

    var body: some View {
        HStack(spacing: 10) {
            ForEach(capabilities, id: \.self) { label in
                StreamBadge(text: label)
            }
            if let fileLine {
                Text(fileLine)
                    .font(.system(size: 22))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .truncationMode(.middle)
                    .padding(.leading, capabilities.isEmpty ? 0 : 6)
            }
        }
        .frame(height: 32)
    }
}

private struct StreamFacts: View {
    let size: String?
    let seeders: Int?
    let isTorrent: Bool

    var body: some View {
        VStack(alignment: .trailing, spacing: 8) {
            if let size {
                Text(size)
                    .font(.system(size: 25, weight: .semibold))
                    .monospacedDigit()
            }
            HStack(spacing: 18) {
                if let seeders {
                    Label("\(seeders)", systemImage: "person.2.fill")
                        .monospacedDigit()
                }
                Label(isTorrent ? "Torrent" : "Direct", systemImage: deliverySymbol)
            }
            .font(.system(size: 21, weight: .medium))
            .foregroundStyle(.secondary)
        }
        .lineLimit(1)
        .fixedSize()
    }

    private var deliverySymbol: String {
        isTorrent ? "arrow.down.circle" : "link"
    }
}
