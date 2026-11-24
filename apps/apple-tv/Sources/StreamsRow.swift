import SwiftUI

struct StreamRow: View {
    private static let height: CGFloat = 104

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
        .padding(.horizontal, 8)
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
        .frame(width: 124, height: 64)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
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
