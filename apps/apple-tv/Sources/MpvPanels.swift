import SwiftUI

struct MpvTrackMenu: View {
    let kind: MpvTrackKind
    let options: [MpvTrackOption]
    let onSelect: (MpvTrackOption) -> Void

    var body: some View {
        Menu {
            if options.isEmpty {
                Text("No \(kind.title.lowercased()) tracks")
            }
            Picker(kind.menuTitle, selection: selection) {
                ForEach(options) { option in
                    MpvTrackMenuItem(option: option)
                        .tag(option.id)
                }
            }
            .pickerStyle(.inline)
        } label: {
            Image(systemName: kind.control.symbol)
        }
        .menuStyle(.button)
        .buttonStyle(MpvCircleButtonStyle(isOpen: false))
        .accessibilityLabel(kind.control.label)
    }

    private var selection: Binding<String> {
        Binding(
            get: { options.first(where: \.isSelected)?.id ?? "" },
            set: { id in
                guard let option = options.first(where: { $0.id == id }) else { return }
                onSelect(option)
            }
        )
    }
}

private struct MpvTrackMenuItem: View {
    let option: MpvTrackOption

    var body: some View {
        Text(option.label)
        if let detail = option.detail {
            Text(detail)
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
