import NuvioTvShared
import SwiftUI

struct DetailsInfoSection: View {
    private static let columnWidth: CGFloat = 400

    let details: TvosMetaDetails
    @FocusState private var isFocused: Bool

    var body: some View {
        if !entries.isEmpty {
            VStack(alignment: .leading, spacing: 20) {
                ShelfHeader(title: "Information")
                HStack(alignment: .top, spacing: 48) {
                    ForEach(entries, id: \.title) { entry in
                        InfoColumn(entry: entry)
                            .frame(maxWidth: Self.columnWidth, alignment: .leading)
                    }
                }
                .padding(28)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .fill(.white.opacity(isFocused ? 0.12 : 0))
                )
                .padding(.horizontal, -28)
                .focusable()
                .focused($isFocused)
                .animation(.easeOut(duration: 0.15), value: isFocused)
            }
        }
    }

    private var entries: [InfoEntry] {
        let candidates: [(String, String?)] = [
            ("Released", details.releaseLine),
            ("Run Time", details.runtime),
            ("Rated", details.ageRating),
            ("Genres", joined(details.genres)),
            ("Director", joined(details.directors)),
        ]
        return candidates.compactMap { title, value in
            value.map { InfoEntry(title: title, value: $0) }
        }
    }

    private func joined(_ values: [String]) -> String? {
        values.isEmpty ? nil : values.joined(separator: ", ")
    }
}

private struct InfoEntry {
    let title: String
    let value: String
}

private struct InfoColumn: View {
    let entry: InfoEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(entry.title)
                .font(.system(size: 22, weight: .semibold))
                .foregroundStyle(.white.opacity(0.5))
            Text(entry.value)
                .font(.system(size: 24))
                .foregroundStyle(.white.opacity(0.85))
                .lineLimit(3)
        }
    }
}
