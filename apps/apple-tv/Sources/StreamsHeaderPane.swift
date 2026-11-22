import SwiftUI
import NuvioTvShared

struct StreamsHeaderPane: View {
    static let width: CGFloat = 560

    let target: TvosStreamTarget
    let resume: TvosResumePoint?
    let isChecking: Bool
    let torrentStatus: TvosTorrentStatus?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            StreamsStill(url: target.episodeThumbnailUrl ?? target.backdropUrl)
                .padding(.bottom, 36)
            StreamsTitleBlock(target: target)
            if let resume {
                StreamsResumeView(resume: resume)
                    .padding(.top, 28)
            }
            status
                .padding(.top, 36)
        }
        .frame(width: Self.width, alignment: .leading)
    }

    @ViewBuilder
    private var status: some View {
        if let torrentStatus {
            TorrentProgressView(status: torrentStatus)
        } else if isChecking {
            CheckingStreamView()
        }
    }
}

private struct StreamsStill: View {
    let url: String?

    var body: some View {
        ArtworkImage(url: url)
            .frame(width: StreamsHeaderPane.width, height: StreamsHeaderPane.width * 9 / 16)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .shadow(color: .black.opacity(0.4), radius: 24, y: 12)
    }
}

private struct StreamsTitleBlock: View {
    let target: TvosStreamTarget

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Streams")
                .font(.system(size: 31, weight: .semibold))
                .foregroundStyle(.white.opacity(0.6))
            Text(target.title)
                .font(.system(size: 48, weight: .bold))
                .lineLimit(2)
            if let subtitle = target.subtitle {
                Text(subtitle)
                    .font(.system(size: 31, weight: .medium))
                    .foregroundStyle(.white.opacity(0.75))
                    .lineLimit(2)
            }
        }
    }
}

private struct StreamsResumeView: View {
    let resume: TvosResumePoint

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            WatchProgressBar(fraction: Double(resume.watchedFraction))
                .frame(width: 360)
            Text(resume.label)
                .font(.system(size: 25, weight: .medium))
                .foregroundStyle(.white.opacity(0.6))
        }
    }
}

private struct CheckingStreamView: View {
    var body: some View {
        HStack(spacing: 24) {
            ProgressView()
            Text("Checking stream")
                .font(.system(size: 29, weight: .semibold))
        }
        .padding(.horizontal, 32)
        .padding(.vertical, 24)
        .darkGlassPanel(cornerRadius: 28)
        .transition(.opacity)
    }
}

private struct TorrentProgressView: View {
    let status: TvosTorrentStatus

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 24) {
                ProgressView()
                Text(status.step ?? "Starting torrent")
                    .font(.system(size: 29, weight: .semibold))
                    .lineLimit(2)
            }
            Text(details)
                .font(.system(size: 25))
                .foregroundStyle(.white.opacity(0.6))
            ProgressView(value: Double(status.bufferFraction))
                .frame(width: 360)
        }
        .padding(.horizontal, 32)
        .padding(.vertical, 24)
        .darkGlassPanel(cornerRadius: 28)
        .transition(.opacity)
    }

    private var details: String {
        "\(status.peers) peers, \(status.seeds) seeds, \(status.downloadSpeedLabel)"
    }
}
