import SwiftUI
import NuvioTvShared

struct StreamsEmptyView: View {
    private static let missingAddonHint =
        "None of your installed addons provide streams. Cinemeta only provides catalogs " +
        "and details. Install a stream addon to see sources here."

    let emptyState: TvosStreamsEmptyState
    let onRetry: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label(emptyState.title, systemImage: "play.slash")
        } description: {
            VStack(spacing: 16) {
                Text(emptyState.message)
                if emptyState.isMissingStreamAddon {
                    Text(Self.missingAddonHint)
                        .foregroundStyle(.secondary)
                }
            }
        } actions: {
            Button("Try Again", systemImage: "arrow.clockwise", action: onRetry)
        }
        .frame(maxWidth: 1000)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct StreamsLoadingView: View {
    var body: some View {
        ProgressView {
            Text("Finding streams")
                .font(.system(size: 29, weight: .medium))
                .foregroundStyle(.secondary)
        }
        .focusable()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
