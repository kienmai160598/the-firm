import SwiftUI
import NuvioTvShared

struct DetailsHeroView: View {
    static let textWidth: CGFloat = 1100

    let details: TvosMetaDetails

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            TitleArtwork(name: details.name, logoUrl: details.logoUrl)
            if !metadataLine.isEmpty {
                Text(metadataLine)
                    .font(.headline)
            }
            if !details.genres.isEmpty {
                Text(details.genres.joined(separator: ", "))
                    .font(.callout)
                    .foregroundStyle(.secondary)
            }
            if let overview = details.overview {
                Text(overview)
                    .font(.body)
                    .lineLimit(3)
            }
            if let credits {
                Text(credits)
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            if let action = details.primaryAction {
                HStack(spacing: 40) {
                    NavigationLink(value: AppRoute.streams(action.target)) {
                        Label(action.label, systemImage: "play.fill")
                    }
                    if let resume = action.resume {
                        ResumeProgressView(resume: resume)
                    }
                }
                .padding(.top, 8)
            }
        }
        .frame(maxWidth: Self.textWidth, alignment: .leading)
        .focusSection()
    }

    private var metadataLine: String {
        let rating = details.imdbRating.map { "IMDb \($0)" }
        return [details.releaseLine, details.runtime, details.ageRating, rating]
            .compactMap { $0 }
            .joined(separator: "  ·  ")
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

private struct ResumeProgressView: View {
    let resume: TvosResumePoint

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(resume.label)
                .font(.callout)
            ProgressView(value: Double(resume.watchedFraction))
                .frame(width: 360)
        }
        .foregroundStyle(.secondary)
    }
}
