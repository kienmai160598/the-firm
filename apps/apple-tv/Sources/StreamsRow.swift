import NuvioTvShared
import SwiftUI

extension TvosStream {
    var features: StreamFeatures {
        StreamFeatures(
            texts: [name, title, detail, filename].compactMap { $0 },
            providedBadges: badges,
            providedSize: sizeLabel
        )
    }

    var oneLineName: String {
        name.split(whereSeparator: \.isNewline)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
            .joined(separator: "  ·  ")
    }

    var fileLine: String? {
        if let filename, !filename.isEmpty { return filename }
        let lines = [title, detail].compactMap { $0 }
            .flatMap { $0.split(whereSeparator: \.isNewline) }
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty && $0 != name }
        return lines.first
    }

    var isTorrent: Bool {
        infoHash != nil
    }
}

struct StreamRow: View {
    private static let headlineWidth: CGFloat = 190
    private static let maxCapabilities = 6

    let stream: TvosStream

    var body: some View {
        let features = stream.features
        HStack(alignment: .center, spacing: 36) {
            StreamHeadline(resolution: features.resolution, size: features.size)
                .frame(width: Self.headlineWidth, alignment: .leading)
            VStack(alignment: .leading, spacing: 10) {
                if !features.capabilities.isEmpty {
                    StreamCapabilities(
                        labels: Array(features.capabilities.prefix(Self.maxCapabilities))
                    )
                }
                Text(stream.oneLineName)
                    .font(.system(size: 27, weight: .semibold))
                    .lineLimit(1)
                if let fileLine = stream.fileLine {
                    Text(fileLine)
                        .font(.system(size: 22))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
            }
            Spacer(minLength: 0)
            StreamDelivery(isTorrent: stream.isTorrent, seeders: features.seeders)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 8)
    }
}

private struct StreamHeadline: View {
    let resolution: String?
    let size: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(resolution ?? "Video")
                .font(.system(size: 44, weight: .heavy))
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(size ?? "Size unknown")
                .font(.system(size: size == nil ? 21 : 27, weight: .semibold))
                .foregroundStyle(size == nil ? .tertiary : .secondary)
                .monospacedDigit()
                .lineLimit(1)
        }
    }
}

private struct StreamCapabilities: View {
    let labels: [String]

    var body: some View {
        HStack(spacing: 10) {
            ForEach(labels, id: \.self) { label in
                StreamCapabilityBadge(text: label)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
    }
}

private struct StreamCapabilityBadge: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 20, weight: .bold))
            .lineLimit(1)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .overlay(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .stroke(lineWidth: 1.5)
            )
            .foregroundStyle(.secondary)
    }
}

private struct StreamDelivery: View {
    let isTorrent: Bool
    let seeders: Int?

    var body: some View {
        VStack(alignment: .trailing, spacing: 6) {
            Label(isTorrent ? "Torrent" : "Direct", systemImage: symbol)
                .font(.system(size: 22, weight: .medium))
            if let seeders {
                Text("\(seeders) seeds")
                    .font(.system(size: 21))
                    .monospacedDigit()
            }
        }
        .foregroundStyle(.secondary)
        .fixedSize()
    }

    private var symbol: String {
        isTorrent ? "arrow.down.circle" : "link"
    }
}
