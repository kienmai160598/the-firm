import AVFoundation
import SwiftUI
import UIKit

/// What the spotlight page shows of its trailer.
struct HeroTrailerPicture {
    let player: AVPlayer
    let isShowing: Bool
    let zoom: CGFloat
}

extension View {
    /// Lays the trailer over a backdrop image. The crop is set while the trailer is hidden, so only
    /// the fade animates.
    func trailerOverlay(_ trailer: HeroTrailerPicture?) -> some View {
        overlay {
            if let trailer {
                HeroTrailerVideo(player: trailer.player, isShowing: trailer.isShowing)
                    .opacity(trailer.isShowing ? 1 : 0)
                    .animation(HeroMotion.trailerFade(isShowing: trailer.isShowing), value: trailer.isShowing)
                    .scaleEffect(trailer.zoom)
            }
        }
    }
}

/// The trailer video, filling the spotlight backdrop the same way the image does.
struct HeroTrailerVideo: UIViewRepresentable {
    static let accessibilityIdentifier = "heroTrailer"

    let player: AVPlayer
    let isShowing: Bool

    func makeUIView(context: Context) -> PlayerView {
        let view = PlayerView()
        view.playerLayer?.videoGravity = .resizeAspectFill
        view.isAccessibilityElement = true
        view.accessibilityIdentifier = Self.accessibilityIdentifier
        view.accessibilityLabel = "Trailer"
        return view
    }

    func updateUIView(_ view: PlayerView, context: Context) {
        view.playerLayer?.player = player
        view.accessibilityValue = isShowing ? "playing" : "hidden"
    }

    final class PlayerView: UIView {
        override class var layerClass: AnyClass { AVPlayerLayer.self }

        var playerLayer: AVPlayerLayer? { layer as? AVPlayerLayer }
    }
}
