import SwiftUI

struct MpvTrackMenu: View {
    private static let maxListHeight: CGFloat = 520
    private static let estimatedRowHeight: CGFloat = 84

    let kind: MpvTrackKind
    let options: [MpvTrackOption]
    let focus: FocusState<MpvFocus?>.Binding
    let onSelect: (MpvTrackOption) -> Void
    @State private var rowsHeight: CGFloat?

    var body: some View {
        MpvMenuPanel(width: 560) {
            MpvMenuHeader(title: kind.menuTitle)
            if options.isEmpty {
                Text("No \(kind.title.lowercased()) tracks")
                    .font(MpvMenuFont.row)
                    .padding(.horizontal, MpvMenuMetrics.rowInset)
                    .padding(.vertical, 18)
            }
            ScrollView {
                rows
                    .padding(.vertical, 4)
                    .onGeometryChange(for: CGFloat.self) { proxy in
                        proxy.size.height
                    } action: { height in
                        rowsHeight = height
                    }
            }
            .frame(height: min(rowsHeight ?? estimatedRowsHeight, Self.maxListHeight))
        }
    }

    private var estimatedRowsHeight: CGFloat {
        CGFloat(options.count) * Self.estimatedRowHeight
    }

    private var rows: some View {
        VStack(alignment: .leading, spacing: 2) {
            ForEach(options) { option in
                Button { onSelect(option) } label: { MpvTrackRow(option: option) }
                    .buttonStyle(MpvMenuRowStyle())
                    .focused(focus, equals: .option(kind, option.id))
            }
        }
    }
}

struct MpvInfoMenu: View {
    let source: String
    let rows: [MpvInfoRow]

    var body: some View {
        MpvMenuPanel(width: 640) {
            MpvMenuHeader(title: "Source")
            Text(source)
                .font(MpvMenuFont.row)
                .lineLimit(2)
                .padding(.horizontal, MpvMenuMetrics.rowInset)
                .padding(.vertical, 10)
            MpvMenuSeparator()
            MpvMenuHeader(title: "Playback")
            ForEach(rows) { row in
                HStack(alignment: .firstTextBaseline, spacing: 24) {
                    Text(row.label)
                        .foregroundStyle(.white.opacity(0.55))
                    Spacer(minLength: 0)
                    Text(row.value)
                        .multilineTextAlignment(.trailing)
                }
                .font(MpvMenuFont.row)
                .padding(.horizontal, MpvMenuMetrics.rowInset)
                .padding(.vertical, 10)
            }
        }
    }
}

private enum MpvMenuMetrics {
    static let rowInset: CGFloat = 24
    static let cornerRadius: CGFloat = 44
}

private enum MpvMenuFont {
    static let row = Font.system(size: 26, weight: .medium)
    static let detail = Font.system(size: 20, weight: .regular)
    static let header = Font.system(size: 23, weight: .medium)
}

private struct MpvMenuPanel<Content: View>: View {
    let width: CGFloat
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            content
        }
        .foregroundStyle(.white)
        .padding(14)
        .frame(width: width, alignment: .leading)
        .darkGlassPanel(cornerRadius: MpvMenuMetrics.cornerRadius)
        .focusSection()
    }
}

private struct MpvMenuHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(MpvMenuFont.header)
            .foregroundStyle(.white.opacity(0.5))
            .padding(.horizontal, MpvMenuMetrics.rowInset)
            .padding(.top, 10)
            .padding(.bottom, 6)
    }
}

private struct MpvMenuSeparator: View {
    var body: some View {
        Rectangle()
            .fill(.white.opacity(0.14))
            .frame(height: 1)
            .padding(.horizontal, MpvMenuMetrics.rowInset)
            .padding(.vertical, 10)
    }
}

private struct MpvTrackRow: View {
    let option: MpvTrackOption

    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: "checkmark")
                .font(.system(size: 24, weight: .semibold))
                .frame(width: 30)
                .opacity(option.isSelected ? 1 : 0)
            VStack(alignment: .leading, spacing: 2) {
                Text(option.label)
                    .font(MpvMenuFont.row)
                    .lineLimit(1)
                if let detail = option.detail {
                    Text(detail)
                        .font(MpvMenuFont.detail)
                        .opacity(0.6)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
    }
}

private struct MpvMenuRowStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        MpvMenuRowBody(configuration: configuration)
    }
}

private struct MpvMenuRowBody: View {
    let configuration: ButtonStyleConfiguration
    @Environment(\.isFocused) private var isFocused

    var body: some View {
        configuration.label
            .foregroundStyle(isFocused ? Color.black : Color.white)
            .padding(.leading, 14)
            .padding(.trailing, MpvMenuMetrics.rowInset)
            .padding(.vertical, 16)
            .background(Capsule().fill(.white.opacity(isFocused ? 1 : 0)))
            .scaleEffect(isFocused ? 1.02 : 1)
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.spring(response: 0.3, dampingFraction: 0.8), value: isFocused)
    }
}

struct MpvCircleButtonStyle: ButtonStyle {
    let isOpen: Bool

    func makeBody(configuration: Configuration) -> some View {
        MpvCircleButtonBody(configuration: configuration, isOpen: isOpen)
    }
}

private struct MpvCircleButtonBody: View {
    private static let diameter: CGFloat = 68

    let configuration: ButtonStyleConfiguration
    let isOpen: Bool
    @Environment(\.isFocused) private var isFocused

    var body: some View {
        configuration.label
            .font(.system(size: 28, weight: .semibold))
            .foregroundStyle(isFocused ? Color.black : Color.white)
            .frame(width: Self.diameter, height: Self.diameter)
            .background { background }
            .scaleEffect(isFocused ? 1.1 : 1)
            .scaleEffect(configuration.isPressed ? 0.94 : 1)
            .shadow(color: .black.opacity(isFocused ? 0.3 : 0), radius: 12, y: 6)
            .animation(.easeOut(duration: 0.15), value: isFocused)
    }

    @ViewBuilder
    private var background: some View {
        if isFocused {
            Circle().fill(.white)
        } else if isOpen {
            Circle().fill(.white.opacity(0.4))
        } else {
            Color.clear.darkGlassPanel(cornerRadius: Self.diameter / 2)
        }
    }
}

extension MpvTrackKind {
    var control: MpvControl {
        switch self {
        case .audio: .audio
        case .subtitles: .subtitles
        }
    }

    var menuTitle: String {
        switch self {
        case .audio: "Audio Track"
        case .subtitles: "Subtitles"
        }
    }
}
