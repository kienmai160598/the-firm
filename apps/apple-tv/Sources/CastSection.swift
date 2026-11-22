import SwiftUI
import NuvioTvShared

struct CastSection: View {
    let cast: [TvosCastMember]

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ShelfHeader(title: "Cast & Crew")
            ScrollView(.horizontal) {
                LazyHStack(alignment: .top, spacing: 40) {
                    ForEach(Array(cast.enumerated()), id: \.offset) { _, member in
                        CastCard(member: member)
                    }
                }
                .padding(.vertical, 30)
            }
            .scrollClipDisabled()
            .focusSection()
        }
    }
}

private struct CastCard: View {
    static let size: CGFloat = 180

    let member: TvosCastMember

    var body: some View {
        VStack(spacing: 16) {
            Button(action: {}) {
                ArtworkImage(url: member.photoUrl)
                    .frame(width: Self.size, height: Self.size)
                    .overlay { initialsIfNoPhoto }
                    .clipShape(Circle())
            }
            .buttonStyle(.card)
            .buttonBorderShape(.circle)
            .accessibilityLabel(member.name)
            Text(member.name)
                .font(.system(size: 24, weight: .medium))
                .lineLimit(2)
                .multilineTextAlignment(.center)
            if let role = member.role {
                Text(role)
                    .font(.system(size: 21))
                    .foregroundStyle(.white.opacity(0.5))
                    .lineLimit(1)
            }
        }
        .frame(width: Self.size + 40)
    }

    @ViewBuilder
    private var initialsIfNoPhoto: some View {
        if member.photoUrl == nil {
            Text(initials)
                .font(.title2)
                .foregroundStyle(.secondary)
        }
    }

    private var initials: String {
        member.name
            .split(separator: " ")
            .prefix(2)
            .compactMap(\.first)
            .map(String.init)
            .joined()
    }
}
