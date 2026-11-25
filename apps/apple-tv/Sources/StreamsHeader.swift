import SwiftUI
import NuvioTvShared

enum StreamsMotion {
    static func animation(reducesMotion: Bool) -> Animation {
        reducesMotion ? .linear(duration: 0.15) : .smooth(duration: 0.35)
    }
}

/// The left column of the Streams screen, as tall as the page: the art fills it, and the title,
/// resume bar and source count sit on its shaded bottom. It stays put while the list scrolls.
struct StreamsHeader: View {
    static let width: CGFloat = 640
    private static let textInset: CGFloat = 40

    let target: TvosStreamTarget
    let resume: TvosResumePoint?
    let sourceCount: Int
    let searchingAddonCount: Int
    /// True while a Continue Watching card's last link loads, before the player covers the list.
    let isResumingLastLink: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
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
            if isResumingLastLink {
                Label { Text("Resuming the last source") } icon: { ProgressView() }
                    .font(.system(size: 25, weight: .medium))
                    .padding(.top, 24)
            } else {
                StreamsSourceSummary(sourceCount: sourceCount, searchingAddonCount: searchingAddonCount)
                    .padding(.top, 24)
            }
        }
        .padding(Self.textInset)
        .frame(width: Self.width, alignment: .leading)
        .frame(maxHeight: .infinity, alignment: .bottom)
        .background { StreamsArtwork(target: target) }
        .clipShape(CardShape.shape)
    }
}

/// The poster, else the episode still, else the backdrop, filling the column; its lower part is
/// shaded so the text on it reads. A plain platter when there is no art.
private struct StreamsArtwork: View {
    private static let shade = LinearGradient(
        stops: [
            .init(color: .black.opacity(0), location: 0.35),
            .init(color: .black.opacity(0.85), location: 1),
        ],
        startPoint: .top,
        endPoint: .bottom
    )

    let target: TvosStreamTarget

    var body: some View {
        ArtworkImage(url: target.posterUrl ?? target.episodeThumbnailUrl ?? target.backdropUrl)
            .overlay { Self.shade }
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
