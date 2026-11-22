import SwiftUI

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
