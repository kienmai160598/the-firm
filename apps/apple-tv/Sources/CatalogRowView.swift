import SwiftUI
import NuvioTvShared

struct CatalogRowView: View {
    let row: TvosRow

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(row.title)
                .font(.title3.weight(.semibold))
                .padding(.horizontal, 80)
            ScrollView(.horizontal) {
                LazyHStack(spacing: 36) {
                    ForEach(row.items, id: \.self) { item in
                        PosterCard(item: item)
                    }
                }
                .padding(.horizontal, 80)
                .padding(.vertical, 30)
            }
            .scrollClipDisabled()
        }
    }
}
