import SwiftUI
import NuvioTvShared

struct StreamGroupList: View {
    let groups: [TvosStreamGroup]
    let onSelect: (TvosStream) -> Void

    var body: some View {
        ScrollView(.vertical) {
            LazyVStack(alignment: .leading, spacing: 48) {
                ForEach(groups, id: \.addonId) { group in
                    StreamGroupSection(group: group, onSelect: onSelect)
                }
            }
            .padding(.vertical, 20)
            .padding(.trailing, 40)
        }
    }
}

private struct StreamGroupSection: View {
    let group: TvosStreamGroup
    let onSelect: (TvosStream) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack(spacing: 20) {
                Text(group.addonName)
                    .font(.title3)
                if group.isLoading {
                    ProgressView()
                }
            }
            groupBody
        }
        .focusSection()
    }

    @ViewBuilder
    private var groupBody: some View {
        if let errorMessage = group.errorMessage {
            Text(errorMessage)
                .font(.body)
                .foregroundStyle(.secondary)
        } else if group.streams.isEmpty && !group.isLoading {
            Text("No streams from this addon")
                .font(.body)
                .foregroundStyle(.secondary)
        } else {
            ForEach(group.streams) { stream in
                Button {
                    onSelect(stream)
                } label: {
                    StreamRow(stream: stream)
                }
            }
        }
    }
}

private struct StreamRow: View {
    let stream: TvosStream

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 16) {
                Text(stream.name)
                    .font(.headline)
                    .lineLimit(2)
                Spacer(minLength: 20)
                ForEach(chips, id: \.self) { chip in
                    StreamChip(text: chip)
                }
            }
            if let detail = stream.detail ?? stream.title {
                Text(detail)
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .lineLimit(3)
                    .multilineTextAlignment(.leading)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 8)
    }

    private var chips: [String] {
        stream.badges + [stream.sizeLabel, stream.kind].compactMap { $0 }
    }
}

private struct StreamChip: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.subheadline.bold())
            .padding(.horizontal, 14)
            .padding(.vertical, 4)
            .overlay(Capsule().strokeBorder(.secondary, lineWidth: 2))
    }
}
