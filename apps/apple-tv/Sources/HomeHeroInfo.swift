import NuvioTvShared
import SwiftUI

/// The title, facts and overview of the current spotlight item. The button sits below it, in
/// HeroFocusRow, so it does not move when the item changes.
struct HeroInfo: View {
    static let maxWidth: CGFloat = 860

    let item: TvosItem

    var body: some View {
        VStack(alignment: .leading, spacing: HeroStage.spacing) {
            HeroTitle(name: item.name, logoUrl: item.logoUrl)
            MetadataLine(facts: facts, imdbRating: item.imdbRating, badgeFontSize: 24)
                .font(.callout.weight(.semibold))
                .foregroundStyle(.white.opacity(0.8))
            if let overview = item.overview {
                Text(overview)
                    .font(.callout)
                    .foregroundStyle(.white.opacity(0.85))
                    .lineLimit(3)
            }
        }
        .frame(maxWidth: Self.maxWidth, alignment: .leading)
    }

    private var facts: [String] {
        let kind = item.type == "series" ? "TV Show" : "Movie"
        let genres = item.genres.prefix(2).joined(separator: ", ")
        return [kind, item.releaseInfo, genres.isEmpty ? nil : genres].compactMap { $0 }
    }
}

private struct HeroTitle: View {
    let name: String
    let logoUrl: String?

    var body: some View {
        AsyncImage(url: logoUrl.flatMap(URL.init(string:))) { phase in
            if let image = phase.image {
                image.resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(maxWidth: 620, maxHeight: 200, alignment: .bottomLeading)
            } else {
                Text(name)
                    .font(.system(size: 76, weight: .bold))
                    .lineLimit(2)
            }
        }
        .accessibilityLabel(name)
    }
}
