import SwiftUI
import NuvioTvShared

enum StreamsMotion {
    static func animation(reducesMotion: Bool) -> Animation {
        reducesMotion ? .linear(duration: 0.15) : .smooth(duration: 0.35)
    }
}

/// The left column of the Streams screen. It stays put while the list beside it scrolls.
struct StreamsHeader: View {
    static let width: CGFloat = 560

    let target: TvosStreamTarget
    let resume: TvosResumePoint?
    let sourceCount: Int
    let searchingAddonCount: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            StreamsArtwork(target: target)
                .padding(.bottom, 26)
            Text(target.title)
                .font(.system(size: 52, weight: .bold))
                .lineLimit(3)
                .minimumScaleFactor(0.7)
                .accessibilityIdentifier("Streams")
            if let subtitle = target.subtitle {
                Text(subtitle)
                    .font(.system(size: 29, weight: .medium))
                    .foregroundStyle(.white.opacity(0.75))
                    .lineLimit(2)
            }
            if let resume {
                StreamsResumeView(resume: resume)
                    .padding(.top, 8)
            }
            StreamsSourceSummary(sourceCount: sourceCount, searchingAddonCount: searchingAddonCount)
                .padding(.top, 24)
        }
        .frame(width: Self.width, alignment: .topLeading)
    }
}

/// The episode still for an episode, otherwise the poster; nothing when neither exists.
private struct StreamsArtwork: View {
    private static let posterSize = CGSize(width: 300, height: 450)
    private static let stillHeight = StreamsHeader.width * 9 / 16

    let target: TvosStreamTarget

    var body: some View {
        if let still = target.episodeThumbnailUrl {
            artwork(still, width: StreamsHeader.width, height: Self.stillHeight)
        } else if let poster = target.posterUrl {
            artwork(poster, width: Self.posterSize.width, height: Self.posterSize.height)
        }
    }

    private func artwork(_ url: String, width: CGFloat, height: CGFloat) -> some View {
        ArtworkImage(url: url, fallbackTitle: target.title)
            .frame(width: width, height: height)
            .clipShape(CardShape.shape)
            .accessibilityHidden(true)
    }
}

private struct StreamsResumeView: View {
    private static let barWidth: CGFloat = 360

    let resume: TvosResumePoint

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            WatchProgressBar(fraction: Double(resume.watchedFraction))
                .frame(width: Self.barWidth)
            Text(resume.label)
                .font(.system(size: 25, weight: .medium))
                .foregroundStyle(.white.opacity(0.6))
        }
    }
}

private struct StreamsSourceSummary: View {
    let sourceCount: Int
    let searchingAddonCount: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            if sourceCount > 0 {
                Text(sourceCount == 1 ? "1 source" : "\(sourceCount) sources")
            }
            if searchingAddonCount > 0 {
                HStack(spacing: 18) {
                    ProgressView()
                    Text(searchingText)
                }
            }
        }
        .font(.system(size: 25, weight: .medium))
        .foregroundStyle(.secondary)
    }

    private var searchingText: String {
        searchingAddonCount == 1
            ? "Searching 1 more addon"
            : "Searching \(searchingAddonCount) more addons"
    }
}

struct StreamsBackground: View {
    private static let top = Color(red: 0.13, green: 0.14, blue: 0.17)
    private static let bottom = Color(red: 0.08, green: 0.09, blue: 0.1)

    let backdropUrl: String?

    var body: some View {
        ZStack {
            LinearGradient(colors: [Self.top, Self.bottom], startPoint: .top, endPoint: .bottom)
            BackdropView(url: backdropUrl, isReceded: true)
        }
        .ignoresSafeArea()
    }
}
