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
