import SwiftUI
import NuvioTvShared

/// Sizes of the Settings app, measured on an Apple TV 4K screenshot (points).
enum SettingsMetrics {
    static let listWidth: CGFloat = 780
    static let gap: CGFloat = 90
    /// The height a pushed page's navigation title takes above the list.
    static let titleRoom: CGFloat = 25
    static let artTop: CGFloat = 92
    static let artSide: CGFloat = 540
    static let artRadius: CGFloat = 120
    static let captionWidth: CGFloat = 720
    /// The caption lines that fit between the art and the bottom safe area.
    static let captionLines = 5
    /// How far a focused row and its shadow reach past the row.
    static let focusGrowth: CGFloat = 16
}

/// The Settings app split: the list against the right safe edge and the preview centred in the
/// space left of it. A tab root has no navigation title, so it leaves the title's room empty and
/// the rows and art sit where they sit on a pushed page.
struct SettingsSplit<Preview: View, Rows: View>: View {
    private let hasTitle: Bool
    private let preview: Preview
    private let list: Rows

    init(hasTitle: Bool = true, @ViewBuilder preview: () -> Preview, @ViewBuilder list: () -> Rows) {
        self.hasTitle = hasTitle
        self.preview = preview()
        self.list = list()
    }

    var body: some View {
        HStack(alignment: .top, spacing: SettingsMetrics.gap) {
            preview
                .padding(.top, SettingsMetrics.artTop)
                .frame(maxWidth: .infinity, alignment: .top)
            list
                .listStyle(.grouped)
                .frame(width: SettingsMetrics.listWidth)
                .mask { scrolledRowsStop }
        }
        .padding(.top, hasTitle ? 0 : SettingsMetrics.titleRoom)
    }

    /// A tvOS list does not clip, so rows scrolled off the top would slide under the title. The mask
    /// stops them at the list's top and leaves room for a focused row to grow and cast its shadow.
    private var scrolledRowsStop: some View {
        Rectangle()
            .padding(.top, -SettingsMetrics.focusGrowth)
            .padding([.horizontal, .bottom], -SettingsMetrics.focusGrowth * 4)
    }
}

/// The preview's art and caption, stacked and centred like Settings.
struct SettingsPreview<Art: View, Caption: View>: View {
    @ViewBuilder let art: Art
    @ViewBuilder let caption: Caption

    var body: some View {
        VStack(spacing: 40) {
            art
                .frame(width: SettingsMetrics.artSide, height: SettingsMetrics.artSide)
            VStack(spacing: 16) {
                caption
            }
            .multilineTextAlignment(.center)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: SettingsMetrics.captionWidth)
        }
    }
}

/// The focused addon: its logo, then why it failed or, unless the page lists it, its description.
struct AddonSummary: View {
    let addon: TvosAddon
    var showsDescription = true

    var body: some View {
        SettingsPreview {
            logo
                .background(.quaternary, in: artShape)
                .clipShape(artShape)
        } caption: {
            if let error = addon.errorMessage {
                Label(error, systemImage: "exclamationmark.triangle.fill")
                    .foregroundStyle(.red)
                    .lineLimit(SettingsMetrics.captionLines)
            } else if showsDescription, !addon.summary.isEmpty {
                Text(addon.summary)
                    .foregroundStyle(.secondary)
                    .lineLimit(SettingsMetrics.captionLines)
            }
        }
    }

    private var artShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: SettingsMetrics.artRadius, style: .continuous)
    }

    private var logo: some View {
        AsyncImage(url: addon.logoUrl.flatMap(URL.init(string:))) { phase in
            if let image = phase.image {
                image.resizable().aspectRatio(contentMode: .fit).padding(90)
            } else {
                Image(systemName: "puzzlepiece.extension")
                    .font(.system(size: 200))
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
