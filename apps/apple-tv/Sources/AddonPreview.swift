import SwiftUI
import NuvioTvShared

/// The Settings app split: a preview of the focused row on the left, the list on the right.
/// The preview is pinned to the top, level with the first row, so a long or short caption does
/// not move the art.
struct SettingsSplit<Preview: View, Rows: View>: View {
    private static var previewWidth: CGFloat { 760 }

    @ViewBuilder let preview: Preview
    @ViewBuilder let list: Rows

    var body: some View {
        HStack(alignment: .top, spacing: 60) {
            preview
                .frame(width: Self.previewWidth, alignment: .top)
            list
        }
    }
}

/// The preview's art and caption, stacked and centred like Settings. The art is sized so ten
/// lines of caption still fit above the bottom safe area.
struct SettingsPreview<Art: View, Caption: View>: View {
    static var artSide: CGFloat { 400 }

    @ViewBuilder let art: Art
    @ViewBuilder let caption: Caption

    var body: some View {
        VStack(spacing: 40) {
            art
                .frame(width: Self.artSide, height: Self.artSide)
            VStack(spacing: 16) {
                caption
            }
            .multilineTextAlignment(.center)
            .fixedSize(horizontal: false, vertical: true)
        }
    }
}

/// The focused addon: its logo, then its summary or why it failed.
struct AddonSummary: View {
    private static let artRadius: CGFloat = 88

    let addon: TvosAddon

    var body: some View {
        SettingsPreview {
            logo
                .background(.quaternary, in: RoundedRectangle(cornerRadius: Self.artRadius, style: .continuous))
                .clipShape(RoundedRectangle(cornerRadius: Self.artRadius, style: .continuous))
        } caption: {
            if let error = addon.errorMessage {
                Label(error, systemImage: "exclamationmark.triangle.fill")
                    .foregroundStyle(.red)
                    .lineLimit(4)
            } else if !addon.summary.isEmpty {
                Text(addon.summary)
                    .foregroundStyle(.secondary)
                    .lineLimit(10)
            }
        }
    }

    private var logo: some View {
        AsyncImage(url: addon.logoUrl.flatMap(URL.init(string:))) { phase in
            if let image = phase.image {
                image.resizable().aspectRatio(contentMode: .fit).padding(66)
            } else {
                Image(systemName: "puzzlepiece.extension")
                    .font(.system(size: 150))
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
