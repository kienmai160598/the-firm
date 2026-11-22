import SwiftUI
import NuvioTvShared

struct StreamsHeader: View {
    let target: TvosStreamTarget
    let resume: TvosResumePoint?
    let isChecking: Bool
    let torrentStatus: TvosTorrentStatus?

    var body: some View {
        HStack(alignment: .bottom, spacing: 40) {
            VStack(alignment: .leading, spacing: 10) {
                Text(target.title)
                    .font(.system(size: 52, weight: .bold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .accessibilityIdentifier("Streams")
                if let subtitle = target.subtitle {
                    Text(subtitle)
                        .font(.system(size: 29, weight: .medium))
                        .foregroundStyle(.white.opacity(0.75))
                        .lineLimit(1)
                }
                if let resume {
                    StreamsResumeView(resume: resume)
                        .padding(.top, 8)
                }
            }
            Spacer(minLength: 0)
            status
        }
        .frame(maxWidth: .infinity, alignment: .leading)
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
