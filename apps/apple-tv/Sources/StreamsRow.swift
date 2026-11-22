import SwiftUI
import NuvioTvShared

struct StreamRow: View {
    let stream: TvosStream

    var body: some View {
        HStack(alignment: .center, spacing: 32) {
            VStack(alignment: .leading, spacing: 8) {
                Text(stream.name)
                    .font(.system(size: 31, weight: .semibold))
                    .lineLimit(1)
                StreamSummaryLine(stream: stream)
            }
            Spacer(minLength: 0)
            StreamBadges(stream: stream)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 2)
    }
}

private struct StreamSummaryLine: View {
    let stream: TvosStream

    var body: some View {
        Text("\(Image(systemName: sourceSymbol)) \(summary)")
            .font(.system(size: 25))
            .foregroundStyle(.secondary)
            .lineLimit(1)
    }

    private var summary: String {
        [stream.kind, oneLineDescription].compactMap { $0 }.joined(separator: "  ·  ")
    }

    private var oneLineDescription: String? {
        guard let description = stream.detail ?? stream.title else { return nil }
        let lines = description
            .split(whereSeparator: \.isNewline)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
        return lines.isEmpty ? nil : lines.joined(separator: "  ·  ")
    }

    private var sourceSymbol: String {
        stream.infoHash != nil ? "arrow.down.circle" : "link"
    }
}

private struct StreamBadges: View {
    let stream: TvosStream

    var body: some View {
        HStack(spacing: 12) {
            ForEach(stream.badges, id: \.self) { badge in
                StreamBadge(text: badge)
            }
            if let sizeLabel = stream.sizeLabel {
                Text(sizeLabel)
                    .font(.system(size: 25, weight: .medium))
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
            }
        }
        .fixedSize()
    }
}

private struct StreamBadge: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 20, weight: .bold))
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .foregroundStyle(.secondary)
            .overlay(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .stroke(.secondary, lineWidth: 1.5)
            )
    }
}
