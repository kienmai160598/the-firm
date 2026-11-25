import NuvioTvShared
import SwiftUI

enum HeroStage {
    static let spacing: CGFloat = 22
    static let bottomInset: CGFloat = 70
    /// The info of the item leaving fades out while the next one fades in, in place.
    static let infoChange: Animation = .easeInOut(duration: 0.6)
}

/// One spotlight page: the backdrop, or its trailer. The artwork runs `artExtension` below the
/// page, under the first shelf. The facts and the button are not part of the page: HeroForeground
/// keeps them in place while the pages slide.
struct HeroPage: View {
    let item: TvosItem
    let trailer: HeroTrailerPicture?
    let artExtension: CGFloat

    var body: some View {
        HeroBackdrop(url: item.backdropUrl, trailer: trailer)
            .padding(.bottom, -artExtension)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }
}

/// The current item's title, facts and overview over the pages, and the button below them. When
/// the item changes, the text crossfades to the new item's and the button stays where it is.
struct HeroForeground: View {
    let items: [TvosItem]
    let currentIndex: Int
    let focusedId: FocusState<String?>.Binding

    var body: some View {
        VStack(alignment: .leading, spacing: HeroStage.spacing) {
            ZStack(alignment: .bottomLeading) {
                HeroInfo(item: items[currentIndex])
                    .id(items[currentIndex].metaId)
                    .transition(.opacity)
            }
            .animation(HeroStage.infoChange, value: currentIndex)
            HeroFocusRow(items: items, currentIndex: currentIndex, focusedId: focusedId)
                .padding(.top, 6)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
        .padding(.horizontal, HomeHero.contentInset)
        .padding(.bottom, HeroStage.bottomInset)
    }
}
