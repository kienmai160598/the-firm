import SwiftUI
import NuvioTvShared

struct StreamsEmptyView: View {
    private static let missingAddonHint =
        "None of your installed addons provide streams. Cinemeta only provides catalogs " +
        "and details. Install a stream addon to see sources here."

    let emptyState: TvosStreamsEmptyState
    let onRetry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 28) {
            Image(systemName: "play.slash.fill")
                .font(.system(size: 72))
                .foregroundStyle(.secondary)
            Text(emptyState.title)
                .font(.title3.bold())
            Text(emptyState.message)
                .font(.body)
            if emptyState.isMissingStreamAddon {
                Text(Self.missingAddonHint)
                    .font(.body)
                    .foregroundStyle(.secondary)
            }
            Button("Try Again", systemImage: "arrow.clockwise", action: onRetry)
                .padding(.top, 12)
        }
        .frame(maxWidth: 1000, alignment: .leading)
    }
}
