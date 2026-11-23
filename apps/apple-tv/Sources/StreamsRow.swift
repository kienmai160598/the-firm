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
            .joined(separator: "   ")
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

struct StreamRowProgress: Equatable {
    let title: String
    let detail: String?
    let fraction: Double?

    static let checking = StreamRowProgress(title: "Checking stream", detail: nil, fraction: nil)
    static let startingTorrent = StreamRowProgress(
        title: "Starting torrent",
        detail: nil,
        fraction: nil
    )
}

struct StreamRow: View {
    private static let height: CGFloat = 104
    private static let maxCapabilities = 4

    let stream: TvosStream
    let progress: StreamRowProgress?

    var body: some View {
        let features = stream.features
        HStack(spacing: 28) {
            StreamResolutionBadge(resolution: features.resolution)
            VStack(alignment: .leading, spacing: 12) {
                Text(stream.oneLineName)
                    .font(.system(size: 29, weight: .semibold))
                    .lineLimit(1)
                StreamDetailLine(
                    capabilities: Array(features.capabilities.prefix(Self.maxCapabilities)),
                    fileLine: stream.fileLine
                )
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            trailing(features)
        }
        .padding(.horizontal, 8)
        .frame(height: Self.height)
    }

    @ViewBuilder
    private func trailing(_ features: StreamFeatures) -> some View {
        if let progress {
            StreamRowProgressView(progress: progress)
                .transition(.opacity)
        } else {
            StreamFacts(size: features.size, seeders: features.seeders, isTorrent: stream.isTorrent)
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
                StreamCapabilityBadge(text: label)
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

private struct StreamCapabilityBadge: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 19, weight: .bold))
            .lineLimit(1)
            .fixedSize()
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .overlay(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .stroke(lineWidth: 1.5)
            )
            .foregroundStyle(.secondary)
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

private struct StreamRowProgressView: View {
    let progress: StreamRowProgress

    var body: some View {
        VStack(alignment: .trailing, spacing: 8) {
            HStack(spacing: 16) {
                ProgressView()
                Text(progress.title)
                    .font(.system(size: 25, weight: .semibold))
                    .lineLimit(1)
            }
            if let fraction = progress.fraction {
                ProgressView(value: fraction)
                    .frame(width: 260)
            }
            if let detail = progress.detail {
                Text(detail)
                    .font(.system(size: 20))
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: 420, alignment: .trailing)
    }
}
