import SwiftUI

/// What a Continue Watching card shows while its last link is checked or its torrent starts: the
/// player's own loading look, black with the spinner, so the card seems to open the player at once
/// and the Streams list underneath only shows when the link fails (AC H4). It takes the focus, so
/// no hidden row can be pressed; Back leaves the screen as it does from Streams.
struct StreamsOpeningCover: View {
    private static let spinnerScale: CGFloat = 1.6

    @FocusState private var isFocused: Bool

    var body: some View {
        ZStack {
            Color.black
            ProgressView()
                .scaleEffect(Self.spinnerScale)
        }
        .ignoresSafeArea()
        .focusable()
        .focused($isFocused)
        .onAppear { isFocused = true }
        .accessibilityIdentifier("streams-opening-cover")
    }
}
