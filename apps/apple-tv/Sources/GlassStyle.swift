import SwiftUI

/// The side padding the system puts between a `.bordered` or `.glass` button's edge and its
/// label, measured at rest on the tvOS 27.2 simulator (edge to text 30 pt, 32 pt to the ink). A
/// wrapper in a column of plain text hangs out by it, so its label starts on the column's edge.
enum SystemButtonInset {
    static let horizontal: CGFloat = 30
}

extension View {
    @ViewBuilder
    func glassPanel(cornerRadius: CGFloat) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        if #available(tvOS 26, *) {
            glassEffect(.regular, in: shape)
        } else {
            background(.ultraThinMaterial, in: shape)
        }
    }

    @ViewBuilder
    func glassCircleButtonStyle() -> some View {
        if #available(tvOS 26, *) {
            buttonStyle(.glass).buttonBorderShape(.circle)
        } else {
            buttonStyle(.bordered).buttonBorderShape(.circle)
        }
    }
}

extension View {
    @ViewBuilder
    func glassProminentButtonStyle() -> some View {
        if #available(tvOS 26, *) {
            buttonStyle(.glassProminent)
        } else {
            buttonStyle(.borderedProminent)
        }
    }

    @ViewBuilder
    func glassButtonStyle() -> some View {
        if #available(tvOS 26, *) {
            buttonStyle(.glass)
        } else {
            buttonStyle(.bordered)
        }
    }
}

extension View {
    @ViewBuilder
    func darkGlassPanel(cornerRadius: CGFloat) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        if #available(tvOS 26, *) {
            glassEffect(.regular, in: shape)
                .environment(\.colorScheme, .dark)
        } else {
            background(.ultraThinMaterial, in: shape)
                .environment(\.colorScheme, .dark)
        }
    }

    @ViewBuilder
    func glassGroup(spacing: CGFloat) -> some View {
        if #available(tvOS 26, *) {
            GlassEffectContainer(spacing: spacing) { self }
        } else {
            self
        }
    }
}
