import SwiftUI

struct PlaybackScreen: View {
    let request: PlaybackRequest
    let onFailure: (StreamNotice) -> Void

    var body: some View {
        switch request.engine {
        case .system:
            PlayerView(request: request, onFailure: onFailure)
                .ignoresSafeArea()
        case .mpv:
            MpvPlayerScreen(request: request, onFailure: onFailure)
        }
    }
}
