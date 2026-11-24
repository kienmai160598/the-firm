import SwiftUI

struct HeroBackdrop: View {
    let url: String?
    let trailer: HeroTrailerPicture?

    var body: some View {
        Color.clear
            .overlay { ArtworkImage(url: url) }
            .trailerOverlay(trailer)
            .clipped()
            .overlay(leadingShade)
            .overlay(bottomShade)
            .mask(bottomFade)
            .ignoresSafeArea(edges: .top)
    }

    /// Keeps the title and the overview readable over any artwork.
    private var leadingShade: some View {
        LinearGradient(
            stops: [
                .init(color: .black.opacity(0.75), location: 0),
                .init(color: .black.opacity(0.25), location: 0.5),
                .init(color: .clear, location: 0.75),
            ],
            startPoint: .leading,
            endPoint: .trailing
        )
    }

    /// Keeps the first shelf readable where it lies over the artwork.
    private var bottomShade: some View {
        LinearGradient(
            stops: [
                .init(color: .clear, location: 0.5),
                .init(color: .black.opacity(0.55), location: 0.85),
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }

    /// Blends the bottom edge into the Home background, which shows once Home scrolls down.
    private var bottomFade: some View {
        LinearGradient(
            stops: [
                .init(color: .black, location: 0.8),
                .init(color: .clear, location: 1),
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}

enum HeroMotion {
    static func trailerFade(isShowing: Bool) -> Animation {
        isShowing
            ? .easeInOut(duration: HeroTrailer.fadeInDuration)
            : .easeInOut(duration: HeroTrailer.fadeOutDuration)
    }
}
