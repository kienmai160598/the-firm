import AVFoundation
import SwiftUI
import UIKit

/// What the spotlight page shows of its trailer.
struct HeroTrailerPicture {
    let player: AVPlayer
    let isShowing: Bool
    let zoom: CGFloat
}

/// The trailer video, filling the spotlight backdrop the same way the image does.
struct HeroTrailerVideo: UIViewRepresentable {
    static let accessibilityIdentifier = "homeHeroTrailer"

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
