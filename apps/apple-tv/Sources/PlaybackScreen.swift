import SwiftUI

struct PlaybackScreen: View {
    let request: PlaybackRequest
    let onFailure: (StreamNotice) -> Void

    var body: some View {
        switch request.engine {
        case .system:
            PlayerView(request: request, onFailure: onFailure)
                .ignoresSafeArea()
                .overlay {
                    if let advisory = request.advisory {
                        PlaybackAdvisoryBanner(advisory: advisory, hasPlaybackStarted: true)
                            .ignoresSafeArea()
                    }
                }
        case .mpv:
            MpvPlayerScreen(request: request, onFailure: onFailure)
        }
    }
}
