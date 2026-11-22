import SwiftUI
import NuvioTvShared

struct StreamGroupList: View {
    let groups: [TvosStreamGroup]
    let onSelect: (TvosStream) -> Void

    var body: some View {
        ScrollView(.vertical) {
            LazyVStack(alignment: .leading, spacing: 48) {
                ForEach(groups, id: \.addonId) { group in
                    StreamGroupSection(
                        group: group,
                        isAwaitingFirstStream: isAwaitingFirstStream,
                        onSelect: onSelect
                    )
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 60)
        }
        .scrollClipDisabled()
        .focusSection()
    }

    private var isAwaitingFirstStream: Bool {
        groups.allSatisfy { $0.streams.isEmpty }
    }
}

private struct StreamGroupSection: View {
    let group: TvosStreamGroup
    let isAwaitingFirstStream: Bool
    let onSelect: (TvosStream) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            StreamGroupHeader(title: group.addonName, isLoading: group.isLoading)
            VStack(alignment: .leading, spacing: 16) {
                rows
            }
        }
    }

    @ViewBuilder
    private var rows: some View {
        if let errorMessage = group.errorMessage {
            StreamGroupMessageRow(text: errorMessage, systemImage: "exclamationmark.triangle")
        } else if group.streams.isEmpty && group.isLoading {
            if isAwaitingFirstStream {
                StreamsLoadingRow()
            }
        } else if group.streams.isEmpty {
            StreamGroupMessageRow(text: "No streams from this addon", systemImage: "tray")
        } else {
            ForEach(group.streams) { stream in
                Button {
                    onSelect(stream)
                } label: {
                    StreamRow(stream: stream)
                }
                .buttonStyle(.bordered)
                .buttonBorderShape(.roundedRectangle(radius: 28))
            }
        }
    }
}

private struct StreamGroupHeader: View {
    let title: String
    let isLoading: Bool

    var body: some View {
        HStack(spacing: 20) {
            Text(title)
                .font(.system(size: 31, weight: .semibold))
                .foregroundStyle(.white.opacity(0.6))
                .accessibilityAddTraits(.isHeader)
            if isLoading {
                ProgressView()
                    .scaleEffect(0.6)
            }
        }
    }
}

private struct StreamGroupMessageRow: View {
    let text: String
    let systemImage: String

    var body: some View {
        Label(text, systemImage: systemImage)
            .font(.system(size: 29))
            .foregroundStyle(.secondary)
    }
}

private struct StreamsLoadingRow: View {
    var body: some View {
        HStack(spacing: 24) {
            ProgressView()
            Text("Finding streams")
                .font(.system(size: 29, weight: .medium))
                .foregroundStyle(.secondary)
        }
        .focusable()
    }
}
