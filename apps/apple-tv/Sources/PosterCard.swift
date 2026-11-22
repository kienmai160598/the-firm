import SwiftUI
import NuvioTvShared

struct PosterCard: View {
    static let width: CGFloat = 220

    let item: TvosItem

    var body: some View {
        NavigationLink(value: AppRoute.details(type: item.type, id: item.metaId)) {
            ArtworkImage(url: item.posterUrl)
                .frame(width: Self.width, height: Self.width * 1.5)
        }
        .buttonStyle(.card)
        .accessibilityLabel(item.name)
    }
}
