import NuvioTvShared
import SwiftUI

struct DetailsHeroView: View {
    private static let descriptionWidth: CGFloat = 820

    let details: TvosMetaDetails
    let isActionFocused: FocusState<Bool>.Binding

    var body: some View {
        VStack(alignment: .leading, spacing: 40) {
            TitleArtwork(name: details.name, logoUrl: details.logoUrl)
            HStack(alignment: .top, spacing: 64) {
                actions
                summary
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .focusSection()
    }

    @ViewBuilder
    private var actions: some View {
        if let action = details.primaryAction {
            VStack(alignment: .leading, spacing: 18) {
                RouteLink(value: AppRoute.streams(action.target)) {
                    Label(action.label, systemImage: "play.fill")
                        .font(.system(size: 30, weight: .semibold))
                        .frame(minWidth: 300)
                        .padding(.vertical, 4)
                }
                .glassProminentButtonStyle()
                .focused(isActionFocused)
                if let resume = action.resume {
                    ResumeProgressView(resume: resume)
                }
            }
        }
    }

    private var summary: some View {
        VStack(alignment: .leading, spacing: 16) {
            if let overview = details.overview {
                Text(overview)
                    .font(.system(size: 27))
                    .foregroundStyle(.white.opacity(0.9))
                    .lineLimit(3)
            }
            MetadataLine(text: metadataLine, badges: badges)
            if let starring {
                Text(starring)
                    .font(.system(size: 23))
                    .foregroundStyle(.white.opacity(0.55))
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: Self.descriptionWidth, alignment: .leading)
    }

    private var metadataLine: String {
        let genres = details.genres.prefix(2).joined(separator: ", ")
        return [genres.isEmpty ? nil : genres, details.releaseLine, details.runtime]
            .compactMap { $0 }
            .joined(separator: " · ")
    }

    private var badges: [String] {
        [details.ageRating, details.imdbRating.map { "IMDb \($0)" }].compactMap { $0 }
    }

    private var starring: String? {
        let names = details.cast.prefix(3).map(\.name)
        guard !names.isEmpty else { return nil }
        return "Starring " + names.joined(separator: ", ")
    }
}

private struct TitleArtwork: View {
    let name: String
    let logoUrl: String?

    var body: some View {
        AsyncImage(url: logoUrl.flatMap(URL.init(string:))) { phase in
            if let image = phase.image {
                image.resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(maxWidth: 720, maxHeight: 220, alignment: .bottomLeading)
            } else {
                Text(name)
                    .font(.system(size: 80, weight: .bold))
                    .lineLimit(2)
                    .minimumScaleFactor(0.6)
                    .frame(maxWidth: 1100, alignment: .leading)
            }
        }
        .accessibilityLabel(name)
    }
}

private struct MetadataLine: View {
    let text: String
    let badges: [String]

    var body: some View {
        HStack(spacing: 12) {
            if !text.isEmpty {
                Text(text)
                    .padding(.trailing, 6)
            }
            ForEach(badges, id: \.self) { badge in
                Text(badge)
                    .font(.system(size: 18, weight: .bold))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .overlay(
                        RoundedRectangle(cornerRadius: 5).stroke(.white.opacity(0.55), lineWidth: 1.5)
                    )
            }
        }
        .font(.system(size: 23, weight: .medium))
        .foregroundStyle(.white.opacity(0.6))
    }
}

private struct ResumeProgressView: View {
    let resume: TvosResumePoint

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            WatchProgressBar(fraction: Double(resume.watchedFraction))
                .frame(width: 300)
            Text(resume.label)
                .font(.system(size: 22))
        }
        .foregroundStyle(.white.opacity(0.6))
    }
}
