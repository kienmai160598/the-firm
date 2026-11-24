import SwiftUI

// Same layout as a hero page, so the screen does not jump when the real hero replaces it. Its
// button exists only to hold focus until then.
struct HeroPlaceholder: View {
    let focusedId: FocusState<String?>.Binding

    var body: some View {
        VStack(alignment: .leading, spacing: 22) {
            SkeletonBar(width: 520, height: 90)
            SkeletonBar(width: 380, height: 28)
            VStack(alignment: .leading, spacing: 12) {
                SkeletonBar(width: 760, height: 24)
                SkeletonBar(width: 640, height: 24)
            }
            Button {} label: {
                Label("Watch Now", systemImage: "play.fill")
                    .font(.body.weight(.semibold))
                    .padding(.horizontal, 8)
                    .redacted(reason: .placeholder)
            }
            .glassProminentButtonStyle()
            .controlSize(.small)
            .focused(focusedId, equals: HomeHero.placeholderFocusId)
            .accessibilityIdentifier("homeHeroPlaceholder")
            .accessibilityLabel("Loading")
            .padding(.top, 6)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
        .padding(.horizontal, HomeHero.contentInset)
        .padding(.bottom, 70)
    }
}

private struct SkeletonBar: View {
    let width: CGFloat
    let height: CGFloat

    var body: some View {
        RoundedRectangle(cornerRadius: 8)
            .fill(.white.opacity(0.12))
            .frame(width: width, height: height)
    }
}

struct PageIndicator: View {
    let count: Int
    let index: Int

    var body: some View {
        HStack(spacing: 10) {
            ForEach(0..<count, id: \.self) { position in
                Capsule()
                    .fill(.white.opacity(position == index ? 1 : 0.35))
                    .frame(width: position == index ? 28 : 10, height: 10)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .glassPanel(cornerRadius: 20)
        .opacity(count > 1 ? 1 : 0)
    }
}
