import SwiftUI
import NuvioTvShared

struct AddonRow: View {
    let addon: TvosAddon

    var body: some View {
        HStack(spacing: 32) {
            AddonLogo(url: addon.logoUrl)
                .opacity(addon.enabled ? 1 : 0.4)
            VStack(alignment: .leading, spacing: 10) {
                titleLine
                detailLine
                if !addon.capabilities.isEmpty {
                    AddonCapabilityBadges(capabilities: addon.capabilities)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            stateLabel
        }
        .padding(.vertical, 12)
    }

    private var titleLine: some View {
        HStack(alignment: .firstTextBaseline, spacing: 16) {
            Text(addon.name)
                .font(.headline)
                .lineLimit(1)
            if !addon.version.isEmpty {
                Text("v\(addon.version)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    @ViewBuilder
    private var detailLine: some View {
        if let error = addon.errorMessage {
            Label(error, systemImage: "exclamationmark.triangle.fill")
                .font(.callout)
                .foregroundStyle(.red)
                .lineLimit(2)
        } else if !addon.summary.isEmpty {
            Text(addon.summary)
                .font(.callout)
                .foregroundStyle(.secondary)
                .lineLimit(2)
        }
    }

    @ViewBuilder
    private var stateLabel: some View {
        if addon.isRefreshing {
            ProgressView()
        } else {
            Text(addon.enabled ? "On" : "Off")
                .font(.callout)
                .foregroundStyle(.secondary)
        }
    }
}

private struct AddonLogo: View {
    let url: String?

    var body: some View {
        AsyncImage(url: url.flatMap(URL.init(string:))) { phase in
            if let image = phase.image {
                image.resizable().aspectRatio(contentMode: .fit)
            } else {
                Image(systemName: "puzzlepiece.extension")
                    .font(.system(size: 40))
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: 96, height: 96)
        .background(.quaternary, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private struct AddonCapabilityBadges: View {
    let capabilities: [String]

    var body: some View {
        HStack(spacing: 12) {
            ForEach(capabilities, id: \.self) { capability in
                Text(capability)
                    .font(.caption2.weight(.semibold))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 4)
                    .overlay(Capsule().strokeBorder(.secondary, lineWidth: 2))
                    .foregroundStyle(.secondary)
            }
        }
    }
}
