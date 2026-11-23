import SwiftUI
import NuvioTvShared

struct StreamSelection: Equatable {
    let streamKey: String
    let progress: StreamRowProgress
}

struct StreamGroupList: View {
    let groups: [TvosStreamGroup]
    let searchingAddonCount: Int
    let selection: StreamSelection?
    let onSelect: (TvosStream) -> Void

    var body: some View {
        ScrollView(.vertical) {
            LazyVStack(alignment: .leading, spacing: 44) {
                ForEach(groups, id: \.addonId) { group in
                    StreamGroupSection(group: group, selection: selection, onSelect: onSelect)
                }
                if searchingAddonCount > 0 {
                    StreamsSearchingFooter(addonCount: searchingAddonCount)
                        .transition(.opacity)
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 60)
        }
        .scrollClipDisabled()
        .focusSection()
    }
}

private struct StreamGroupSection: View {
    let group: TvosStreamGroup
    let selection: StreamSelection?
    let onSelect: (TvosStream) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text(group.addonName)
                .font(.system(size: 27, weight: .semibold))
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .padding(.leading, 8)
                .accessibilityAddTraits(.isHeader)
            if let errorMessage = group.errorMessage, group.streams.isEmpty {
                Label(errorMessage, systemImage: "exclamationmark.triangle")
                    .font(.system(size: 25))
                    .foregroundStyle(.secondary)
                    .padding(.leading, 8)
            }
            VStack(alignment: .leading, spacing: 14) {
                ForEach(group.streams) { stream in
                    StreamButton(
                        stream: stream,
                        progress: progress(for: stream),
                        isDimmed: isDimmed(stream),
                        onSelect: onSelect
                    )
                }
            }
        }
    }

    private func progress(for stream: TvosStream) -> StreamRowProgress? {
        selection?.streamKey == stream.key ? selection?.progress : nil
    }

    private func isDimmed(_ stream: TvosStream) -> Bool {
        guard let selection else { return false }
        return selection.streamKey != stream.key
    }
}

private struct StreamButton: View {
    let stream: TvosStream
    let progress: StreamRowProgress?
    let isDimmed: Bool
    let onSelect: (TvosStream) -> Void

    var body: some View {
        Button {
            onSelect(stream)
        } label: {
            StreamRow(stream: stream, progress: progress)
        }
        .buttonStyle(.bordered)
        .buttonBorderShape(.roundedRectangle(radius: 24))
        .opacity(isDimmed ? 0.45 : 1)
    }
}

private struct StreamsSearchingFooter: View {
    let addonCount: Int

    var body: some View {
        HStack(spacing: 18) {
            ProgressView()
            Text(text)
                .font(.system(size: 25, weight: .medium))
                .foregroundStyle(.secondary)
        }
        .padding(.leading, 8)
    }

    private var text: String {
        addonCount == 1 ? "Searching 1 more addon" : "Searching \(addonCount) more addons"
    }
}
