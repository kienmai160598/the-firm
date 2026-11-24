import SwiftUI

/// One outlined tag (HDR10, Dolby Atmos, HEVC), as the Streams rows and the player's Info show it.
struct StreamBadge: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 19, weight: .bold))
            .lineLimit(1)
            .fixedSize()
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .overlay(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .stroke(lineWidth: 1.5)
            )
            .foregroundStyle(.secondary)
    }
}

/// Tags that wrap to a new line instead of running out of a narrow panel.
struct StreamBadgeFlow: View {
    let badges: [String]

    var body: some View {
        BadgeFlowLayout(spacing: 10) {
            ForEach(badges, id: \.self) { StreamBadge(text: $0) }
        }
    }
}

private struct BadgeFlowLayout: Layout {
    let spacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let frames = frames(for: subviews, width: proposal.width ?? .infinity)
        let width = frames.map(\.maxX).max() ?? 0
        let height = frames.map(\.maxY).max() ?? 0
        return CGSize(width: width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let frames = frames(for: subviews, width: bounds.width)
        for (subview, frame) in zip(subviews, frames) {
            let origin = CGPoint(x: bounds.minX + frame.minX, y: bounds.minY + frame.minY)
            subview.place(at: origin, proposal: ProposedViewSize(frame.size))
        }
    }

    /// Lays the tags out left to right and starts a new line when the next one does not fit.
    private func frames(for subviews: Subviews, width: CGFloat) -> [CGRect] {
        var frames: [CGRect] = []
        var origin = CGPoint.zero
        var lineHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if origin.x > 0, origin.x + size.width > width {
                origin = CGPoint(x: 0, y: origin.y + lineHeight + spacing)
                lineHeight = 0
            }
            frames.append(CGRect(origin: origin, size: size))
            origin.x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
        return frames
    }
}
