import NuvioTvShared
import SwiftUI

struct CatalogRowView: View {
    let row: TvosRow

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ShelfHeader(title: row.title)
            ScrollView(.horizontal) {
                LazyHStack(spacing: ShelfMetrics.spacing) {
                    ForEach(row.items, id: \.self) { item in
                        PosterCard(item: item)
                    }
                }
                .padding(.horizontal, ShelfMetrics.inset)
                .padding(.vertical, 34)
            }
            .scrollClipDisabled()
        }
    }
}

enum ShelfMetrics {
    static let inset: CGFloat = 0
    static let spacing: CGFloat = 40
}

struct ShelfHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(.system(size: 31, weight: .semibold))
            .foregroundStyle(.white.opacity(0.6))
            .padding(.horizontal, ShelfMetrics.inset)
    }
}
