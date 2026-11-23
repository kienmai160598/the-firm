import SwiftUI

struct MetadataLine: View {
    let facts: [String]
    var ageRating: String?
    var imdbRating: String?
    var badgeFontSize: CGFloat = 19

    var body: some View {
        HStack(spacing: 20) {
            if !facts.isEmpty {
                FactLine(facts: facts)
            }
            if let ageRating {
                RatingTag(text: ageRating, fontSize: badgeFontSize)
            }
            if let imdbRating {
                ImdbBadge(rating: imdbRating, fontSize: badgeFontSize)
            }
        }
    }
}

struct FactLine: View {
    let facts: [String]

    var body: some View {
        HStack(spacing: 16) {
            ForEach(Array(facts.enumerated()), id: \.offset) { index, fact in
                if index > 0 {
                    FactDivider()
                }
                Text(fact)
                    .lineLimit(1)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
    }
}

private struct FactDivider: View {
    var body: some View {
        Capsule()
            .fill(.white.opacity(0.35))
            .frame(width: 2)
            .padding(.vertical, 5)
    }
}

struct ImdbBadge: View {
    private static let imdbYellow = Color(red: 245 / 255, green: 197 / 255, blue: 24 / 255)

    let rating: String
    var fontSize: CGFloat = 19

    var body: some View {
        HStack(spacing: 10) {
            Text("IMDb")
                .font(.system(size: fontSize, weight: .black))
                .foregroundStyle(.black)
                .padding(.horizontal, fontSize * 0.4)
                .padding(.vertical, fontSize * 0.12)
                .background(
                    Self.imdbYellow,
                    in: RoundedRectangle(cornerRadius: fontSize * 0.25, style: .continuous)
                )
            Text(rating)
                .fontWeight(.semibold)
                .foregroundStyle(.white)
        }
        .accessibilityElement(children: .combine)
    }
}

private struct RatingTag: View {
    let text: String
    let fontSize: CGFloat

    var body: some View {
        Text(text)
            .font(.system(size: fontSize, weight: .bold))
            .padding(.horizontal, fontSize * 0.4)
            .padding(.vertical, fontSize * 0.12)
            .overlay(
                RoundedRectangle(cornerRadius: fontSize * 0.25, style: .continuous)
                    .stroke(.white.opacity(0.55), lineWidth: 1.5)
            )
    }
}
