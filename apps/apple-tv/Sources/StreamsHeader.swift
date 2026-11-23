import SwiftUI
import NuvioTvShared

struct StreamsHeader: View {
    let target: TvosStreamTarget
    let resume: TvosResumePoint?

    var body: some View {
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
        .padding(.horizontal, 32)
        .frame(maxWidth: .infinity, alignment: .leading)
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
