import SwiftUI

struct StreamRowProgress: Equatable {
    let title: String
    let detail: String?

    static let checking = StreamRowProgress(title: "Checking stream", detail: nil)
    static let startingTorrent = StreamRowProgress(title: "Starting torrent", detail: nil)
}

/// The trailing side of the row being checked. It alone watches the torrent, so a status tick
/// redraws this view and nothing else on the screen.
struct StreamCheckingStatus: View {
    let streamKey: String
    @ObservedObject var torrent: TorrentSession

    var body: some View {
        StreamRowProgressView(progress: progress)
    }

    private var progress: StreamRowProgress {
        guard torrent.startingStreamKey == streamKey else { return .checking }
        guard let status = torrent.status else { return .startingTorrent }
        return StreamRowProgress(
            title: status.step ?? StreamRowProgress.startingTorrent.title,
            detail: "\(status.peers) peers · \(status.seeds) seeds · \(status.downloadSpeedLabel)"
        )
    }
}

private struct StreamRowProgressView: View {
    private static let maxWidth: CGFloat = 300

    let progress: StreamRowProgress

    var body: some View {
        VStack(alignment: .trailing, spacing: 8) {
            HStack(spacing: 16) {
                ProgressView()
                Text(progress.title)
                    .font(.system(size: 25, weight: .semibold))
                    .lineLimit(1)
                    .truncationMode(.tail)
            }
            if let detail = progress.detail {
                Text(detail)
                    .font(.system(size: 20))
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: Self.maxWidth, alignment: .trailing)
    }
}
