import SwiftUI

enum CardShape {
    static let cornerRadius: CGFloat = 24
    static let contentInset: CGFloat = 20
    static let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
}

extension View {
    @ViewBuilder
    func cardArtworkShape() -> some View {
        let clipped = clipShape(CardShape.shape)
        if #available(tvOS 18, *) {
            clipped
                .contentShape(.hoverEffect, CardShape.shape)
                .hoverEffect(.highlight)
        } else {
            clipped.hoverEffect(.highlight)
        }
    }

    func cardButtonShape() -> some View {
        buttonBorderShape(.roundedRectangle(radius: CardShape.cornerRadius))
    }
}
