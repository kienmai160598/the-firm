import SwiftUI
import NuvioTvShared

struct DetailsHeroView: View {
    static let textWidth: CGFloat = 1100

    let details: TvosMetaDetails

    var body: some View {
        VStack(alignment: .leading, spacing: 22) {
            TitleArtwork(name: details.name, logoUrl: details.logoUrl)
            MetadataLine(text: metadataLine, ageRating: details.ageRating)
            if let overview = details.overview {
                Text(overview)
                    .font(.callout)
                    .foregroundStyle(.white.opacity(0.85))
                    .lineLimit(3)
            }
            if let credits {
                Text(credits)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            if let action = details.primaryAction {
                HStack(alignment: .center, spacing: 36) {
                    RouteLink(value: AppRoute.streams(action.target)) {
                        Label(action.label, systemImage: "play.fill")
                            .font(.headline)
                            .padding(.horizontal, 12)
                    }
                    .glassProminentButtonStyle()
                    if let resume = action.resume {
                        ResumeProgressView(resume: resume)
                    }
                }
                .padding(.top, 6)
            }
        }
        .frame(maxWidth: Self.textWidth, alignment: .leading)
        .focusSection()
    }

    private var metadataLine: String {
        let kind = details.type == "series" ? "TV Show" : "Movie"
        let genres = details.genres.prefix(3).joined(separator: ", ")
        let rating = details.imdbRating.map { "IMDb \($0)" }
        return [kind, genres.isEmpty ? nil : genres, details.releaseLine, details.runtime, rating]
            .compactMap { $0 }
            .joined(separator: " · ")
    }

    private var credits: String? {
        guard !details.directors.isEmpty else { return nil }
        return "Director: " + details.directors.joined(separator: ", ")
    }
}

private struct TitleArtwork: View {
    let name: String
    let logoUrl: String?

    var body: some View {
        if let logoUrl, let url = URL(string: logoUrl) {
            AsyncImage(url: url) { phase in
                if let image = phase.image {
                    image.resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(maxWidth: 640, maxHeight: 150, alignment: .leading)
                } else {
                    titleText
                }
            }
            .accessibilityLabel(name)
        } else {
            titleText
        }
    }

    private var titleText: some View {
        Text(name)
            .font(.largeTitle.bold())
            .lineLimit(2)
    }
}

private struct MetadataLine: View {
    let text: String
    let ageRating: String?

    var body: some View {
        HStack(spacing: 14) {
            Text(text)
            if let ageRating {
                Text(ageRating)
                    .font(.caption2.weight(.bold))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .overlay(
                        RoundedRectangle(cornerRadius: 6).stroke(.white.opacity(0.6), lineWidth: 2)
                    )
            }
        }
        .font(.callout.weight(.semibold))
        .foregroundStyle(.white.opacity(0.8))
    }
}

private struct ResumeProgressView: View {
    let resume: TvosResumePoint

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(resume.label)
                .font(.caption)
            WatchProgressBar(fraction: Double(resume.watchedFraction))
                .frame(width: 300)
        }
        .foregroundStyle(.secondary)
    }
}
