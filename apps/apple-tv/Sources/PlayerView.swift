import AVKit
import SwiftUI

struct PlayerView: UIViewControllerRepresentable {
    let request: PlaybackRequest
    let onFailure: (StreamNotice) -> Void

    func makeCoordinator() -> PlaybackController {
        PlaybackController(request: request, onFailure: onFailure)
    }

    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let controller = AVPlayerViewController()
        controller.player = context.coordinator.player
        return controller
    }

    func updateUIViewController(_ controller: AVPlayerViewController, context: Context) {}

    static func dismantleUIViewController(
        _ controller: AVPlayerViewController,
        coordinator: PlaybackController
    ) {
        coordinator.finish()
        controller.player = nil
    }
}
