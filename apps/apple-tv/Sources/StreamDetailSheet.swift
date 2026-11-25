import SwiftUI
import NuvioTvShared

struct StreamSheet: Identifiable {
    let stream: TvosStream
    let notice: StreamNotice?

    var id: String { stream.key + (notice?.title ?? "") }
}

struct StreamDetailSheet: View {
    private static let torrentNote =
        "This is a torrent. Movies on Apple TV streams it with its built-in torrent engine " +
        "and plays MP4, MKV, WebM and most other video files."
    private static let unsupportedNote =
        "Movies on Apple TV cannot play this source yet. It plays torrents and direct " +
        "HTTP(S) links such as MP4, MKV and HLS."

    let stream: TvosStream
    var notice: StreamNotice?

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: 36) {
            if let notice {
                StreamNoticeView(notice: notice)
            }
            Text(stream.name)
                .font(.system(size: 48, weight: .bold))
                .lineLimit(2)
            StreamFieldGrid(fields: fields)
            if notice == nil {
                Text(playbackNote)
                    .font(.system(size: 25))
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Button("Close", action: { dismiss() })
                .padding(.top, 8)
                .padding(.leading, -SystemButtonInset.horizontal)
        }
        .frame(maxWidth: 1300, alignment: .leading)
        .padding(64)
    }

    private var playbackNote: String {
        stream.infoHash != nil ? Self.torrentNote : Self.unsupportedNote
    }

    private var fields: [(label: String, value: String)] {
        let candidates: [(String, String?)] = [
            ("Source", stream.kind),
            ("Details", stream.detail ?? stream.title),
            ("URL", stream.url),
            ("External URL", stream.externalUrl),
            ("Info hash", stream.infoHash),
            ("File index", stream.fileIndex),
            ("File name", stream.filename),
            ("Size", stream.sizeLabel),
        ]
        return candidates.compactMap { label, value in
            value.map { (label: label, value: $0) }
        }
    }
}

private struct StreamNoticeView: View {
    let notice: StreamNotice

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline, spacing: 16) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .symbolRenderingMode(.multicolor)
                Text(notice.title)
            }
            .font(.system(size: 38, weight: .semibold))
            Text(notice.message)
                .font(.system(size: 29))
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

private struct StreamFieldGrid: View {
    let fields: [(label: String, value: String)]

    var body: some View {
        Grid(alignment: .leadingFirstTextBaseline, horizontalSpacing: 40, verticalSpacing: 20) {
            ForEach(fields, id: \.label) { field in
                GridRow {
                    Text(field.label)
                        .font(.system(size: 25, weight: .medium))
                        .foregroundStyle(.secondary)
                        .gridColumnAlignment(.leading)
                    Text(field.value)
                        .font(.system(size: 29))
                        .lineLimit(4)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
        }
    }
}
