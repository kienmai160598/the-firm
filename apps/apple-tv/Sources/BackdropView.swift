import SwiftUI

struct BackdropView: View {
    let url: String?
    var dimming: Double = 0
    var isReceded = false

    var body: some View {
        Color.clear
            .overlay { ArtworkImage(url: url) }
            .clipped()
            .blur(radius: isReceded ? 40 : 0)
            .overlay(Color.black.opacity(isReceded ? 0.45 : dimming))
            .overlay(leadingShade)
            .overlay(bottomShade)
            .ignoresSafeArea()
    }

    private var leadingShade: some View {
        LinearGradient(
            stops: [
                .init(color: .black.opacity(0.9), location: 0),
                .init(color: .black.opacity(0.55), location: 0.45),
                .init(color: .clear, location: 0.8),
            ],
            startPoint: .leading,
            endPoint: .trailing
        )
    }

    private var bottomShade: some View {
        LinearGradient(
            stops: [
                .init(color: .clear, location: 0.35),
                .init(color: .black.opacity(0.85), location: 0.8),
                .init(color: .black, location: 1),
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}
