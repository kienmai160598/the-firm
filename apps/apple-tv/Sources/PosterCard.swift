import NuvioTvShared
import SwiftUI

struct PosterCard: View {
    static let width: CGFloat = 260
    static let height: CGFloat = 390
    static let cornerRadius: CGFloat = 22

    let item: TvosItem

    var body: some View {
        RouteLink(value: AppRoute.details(type: item.type, id: item.metaId)) {
            PosterArtwork(item: item)
        }
        .buttonStyle(.card)
        .accessibilityLabel(item.name)
    }
}

private struct PosterArtwork: View {
    private static let shadeHeight: CGFloat = 130

    let item: TvosItem
    @Environment(\.isFocused) private var isFocused

    var body: some View {
        ArtworkImage(url: item.posterUrl)
            .frame(width: PosterCard.width, height: PosterCard.height)
            .overlay(alignment: .bottom) {
                if let genre = item.genres.first {
                    genreLabel(genre)
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: PosterCard.cornerRadius, style: .continuous))
    }

    private func genreLabel(_ genre: String) -> some View {
        Text(genre)
            .font(.system(size: 26, weight: .medium))
            .foregroundStyle(.white.opacity(isFocused ? 1 : 0.72))
            .lineLimit(1)
            .padding(.horizontal, 16)
            .padding(.bottom, 22)
            .frame(maxWidth: .infinity, minHeight: Self.shadeHeight, alignment: .bottom)
            .background(
                LinearGradient(
                    stops: [
                        .init(color: .clear, location: 0),
                        .init(color: .black.opacity(0.6), location: 0.5),
                        .init(color: .black.opacity(0.88), location: 1),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
    }
}
