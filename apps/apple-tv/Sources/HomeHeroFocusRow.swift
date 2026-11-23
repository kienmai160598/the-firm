import NuvioTvShared
import SwiftUI

/// The spotlight's one visible button, with an invisible focus catcher for the item on each side.
/// Left or right moves focus onto a catcher, HomeHero makes that item current, and the catcher
/// turns into the button in the same place, so the button stays put while the page changes. The
/// first item has no catcher on its left, so left from it still reaches the sidebar.
struct HeroFocusRow: View {
    static let buttonIdentifier = "homeHeroButton"
    static let catcherIdentifier = "homeHeroNeighbour"
    /// Near zero, not zero: the focus engine skips views it considers hidden.
    private static let catcherOpacity = 0.02

    let items: [TvosItem]
    let currentIndex: Int
    let focusedId: FocusState<String?>.Binding

    var body: some View {
        HeroFocusLayout(currentPosition: currentIndex > 0 ? 1 : 0) {
            // Keyed by item, so a catcher keeps its identity, and focus, as it becomes the button.
            ForEach(window, id: \.metaId) { item in
                button(for: item, isCurrent: item.metaId == items[currentIndex].metaId)
            }
        }
        .transaction { $0.animation = nil }
    }

    private var window: [TvosItem] {
        Array(items[max(currentIndex - 1, 0)...min(currentIndex + 1, items.count - 1)])
    }

    private func button(for item: TvosItem, isCurrent: Bool) -> some View {
        RouteLink(value: AppRoute.details(type: item.type, id: item.metaId)) {
            if isCurrent {
                Label("Watch Now", systemImage: "play.fill")
                    .font(.body.weight(.semibold))
                    .padding(.horizontal, 8)
            } else {
                Color.clear.frame(width: 0)
            }
        }
        .glassProminentButtonStyle()
        .controlSize(.small)
        .opacity(isCurrent ? 1 : Self.catcherOpacity)
        .focused(focusedId, equals: item.metaId)
        .accessibilityIdentifier(isCurrent ? Self.buttonIdentifier : Self.catcherIdentifier)
        .accessibilityValue(item.name)
    }
}

/// Sizes the row to the current button alone and puts the catchers just outside its edges, so
/// they never move the button or overlap it.
private struct HeroFocusLayout: Layout {
    let currentPosition: Int

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        guard subviews.indices.contains(currentPosition) else { return .zero }
        return subviews[currentPosition].sizeThatFits(.unspecified)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        for (position, subview) in subviews.enumerated() {
            let size = ProposedViewSize(subview.sizeThatFits(.unspecified))
            if position < currentPosition {
                subview.place(at: CGPoint(x: bounds.minX, y: bounds.midY), anchor: .trailing, proposal: size)
            } else if position == currentPosition {
                subview.place(at: bounds.origin, anchor: .topLeading, proposal: size)
            } else {
                subview.place(at: CGPoint(x: bounds.maxX, y: bounds.midY), anchor: .leading, proposal: size)
            }
        }
    }
}
