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
            MpvMenuHeader(title: kind.menuTitle, leading: MpvMenuMetrics.trackLabelLeading)
            if options.isEmpty {
                Text("No \(kind.title.lowercased()) tracks")
                    .font(MpvMenuFont.row)
                    .padding(.leading, MpvMenuMetrics.trackLabelLeading)
                    .padding(.trailing, MpvMenuMetrics.rowInset)
                    .padding(.vertical, 18)
            }
            ScrollView {
                rows
                    .padding(.vertical, 6)
                    .padding(.horizontal, MpvMenuMetrics.rowsInset)
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
    let source: MpvSourceInfo
    let rows: [MpvInfoRow]

    var body: some View {
        MpvMenuPanel(width: 640) {
            MpvMenuHeader(title: "Source")
            MpvSourceSummary(source: source)
            MpvMenuSeparator()
            MpvMenuHeader(title: "Playback")
            ForEach(rows) { row in
                HStack(alignment: .firstTextBaseline, spacing: 24) {
                    Text(row.label)
                        .foregroundStyle(.secondary)
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

private struct MpvSourceSummary: View {
    let source: MpvSourceInfo

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(source.title)
                .font(MpvMenuFont.row)
                .lineLimit(2)
            if !source.badges.isEmpty {
                StreamBadgeFlow(badges: source.badges)
                    .padding(.leading, -StreamBadge.textInset)
            }
            Text(source.detail)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .font(MpvMenuFont.detail)
        .padding(.horizontal, MpvMenuMetrics.rowInset)
        .padding(.vertical, 10)
    }
}

private enum MpvMenuMetrics {
    static let rowInset: CGFloat = 24
    static let rowsInset: CGFloat = 4
    static let rowLeading: CGFloat = 14
    static let checkWidth: CGFloat = 30
    static let checkGap: CGFloat = 16
    /// Where a track row's label starts. The header and the empty line start there too, so plain
    /// text and the text inside the rows share one edge and the focused capsule hangs out past it.
    static let trackLabelLeading = rowsInset + rowLeading + checkWidth + checkGap
    static let rowVertical: CGFloat = 16
    static let panelPadding: CGFloat = 14
    /// Concentric rule, outer radius = inner radius + padding: a one line row is a capsule, so the
    /// panel's corner is half its height plus the panel's padding (about 45.5 pt).
    static let cornerRadius = (rowVertical * 2 + MpvMenuFont.rowLineHeight) / 2 + panelPadding
}

private enum MpvMenuFont {
    private static let rowSize: CGFloat = 26
    static let row = Font.system(size: rowSize, weight: .medium)
    static let rowLineHeight = UIFont.systemFont(ofSize: rowSize, weight: .medium).lineHeight
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
        .foregroundStyle(.primary)
        .padding(MpvMenuMetrics.panelPadding)
        .frame(width: width, alignment: .leading)
        .darkGlassPanel(cornerRadius: MpvMenuMetrics.cornerRadius)
        .focusSection()
    }
}

private struct MpvMenuHeader: View {
    let title: String
    var leading = MpvMenuMetrics.rowInset

    var body: some View {
        Text(title)
            .font(MpvMenuFont.header)
            .foregroundStyle(.secondary)
            .padding(.leading, leading)
            .padding(.trailing, MpvMenuMetrics.rowInset)
            .padding(.top, 10)
            .padding(.bottom, 6)
    }
}

private struct MpvMenuSeparator: View {
    var body: some View {
        Rectangle()
            .fill(.separator)
            .frame(height: 1)
            .padding(.horizontal, MpvMenuMetrics.rowInset)
            .padding(.vertical, 10)
    }
}

private struct MpvTrackRow: View {
    let option: MpvTrackOption

    var body: some View {
        HStack(spacing: MpvMenuMetrics.checkGap) {
            Image(systemName: "checkmark")
                .font(.system(size: 24, weight: .semibold))
                .frame(width: MpvMenuMetrics.checkWidth)
                .opacity(option.isSelected ? 1 : 0)
            VStack(alignment: .leading, spacing: 2) {
                Text(option.label)
                    .font(MpvMenuFont.row)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
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
            .foregroundStyle(isFocused ? AnyShapeStyle(Color.black) : AnyShapeStyle(.primary))
            .padding(.leading, MpvMenuMetrics.rowLeading)
            .padding(.trailing, MpvMenuMetrics.rowInset)
            .padding(.vertical, MpvMenuMetrics.rowVertical)
            .background(Capsule().fill(.white.opacity(isFocused ? 1 : 0)))
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.spring(response: 0.3, dampingFraction: 0.8), value: isFocused)
    }
}

extension View {
    func mpvCircleButtonStyle(isOpen: Bool) -> some View {
        glassCircleButtonStyle()
            .tint(isOpen ? Color.white.opacity(0.4) : nil)
            .font(.system(size: 28, weight: .semibold))
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
