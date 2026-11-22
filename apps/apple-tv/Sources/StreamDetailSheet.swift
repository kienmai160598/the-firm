import SwiftUI
import NuvioTvShared

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
        VStack(alignment: .leading, spacing: 28) {
            if let notice {
                StreamNoticeView(notice: notice)
            }
            Text(stream.name)
                .font(.title2.bold())
            ForEach(fields, id: \.label) { field in
                StreamField(label: field.label, value: field.value)
            }
            if notice == nil {
                Text(playbackNote)
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Button("Close", action: { dismiss() })
                .padding(.top, 12)
        }
        .frame(maxWidth: 1400, alignment: .leading)
        .padding(60)
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
        HStack(alignment: .top, spacing: 28) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 56))
                .foregroundStyle(.yellow)
            VStack(alignment: .leading, spacing: 12) {
                Text(notice.title)
                    .font(.title3.bold())
                Text(notice.message)
                    .font(.body)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.bottom, 12)
    }
}

private struct StreamField: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.callout)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.body)
                .lineLimit(4)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}
