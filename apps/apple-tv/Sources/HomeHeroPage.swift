import NuvioTvShared
import SwiftUI

enum HeroStage {
    static let spacing: CGFloat = 22
    static let bottomInset: CGFloat = 70
}

/// One spotlight page: the backdrop, or its trailer, behind the item's facts and its button. The
/// artwork runs `artExtension` below the page, under the first shelf.
struct HeroPage: View {
    let item: TvosItem
    let trailer: HeroTrailerPicture?
    let artExtension: CGFloat
    let focusedId: FocusState<String?>.Binding

    var body: some View {
        VStack(alignment: .leading, spacing: HeroStage.spacing) {
            HeroInfo(item: item)
            button
                .padding(.top, 6)
                .padding(.leading, -Self.buttonContentInset)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
        .padding(.horizontal, HomeHero.contentInset)
        .padding(.bottom, HeroStage.bottomInset)
        .background(alignment: .top) {
            HeroBackdrop(url: item.backdropUrl, trailer: trailer)
                .padding(.bottom, -artExtension)
        }
    }

    /// How far the play glyph sits inside the capsule: the system glass padding plus the label's own
    /// 8 pt, measured on the simulator (2026-11-25, unfocused 40 pt to the glyph's ink). The button
    /// hangs out by this much, so the glyph starts on the text's edge as Apple aligns them.
    private static let buttonContentInset: CGFloat = 40

    private var button: some View {
        RouteLink(value: AppRoute.details(type: item.type, id: item.metaId)) {
            Label("Watch Now", systemImage: "play.fill")
                .font(.body.weight(.semibold))
                .padding(.horizontal, 8)
        }
        .glassProminentButtonStyle()
        .controlSize(.small)
        .focused(focusedId, equals: item.metaId)
        .accessibilityIdentifier(HeroFocusRow.buttonIdentifier)
        .accessibilityValue(item.name)
    }
}
