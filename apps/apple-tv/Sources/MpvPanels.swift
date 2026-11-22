import SwiftUI

struct MpvTrackMenu: View {
    let kind: MpvTrackKind
    let options: [MpvTrackOption]
    let focus: FocusState<MpvFocus?>.Binding
    let onSelect: (MpvTrackOption) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(kind.title)
                .font(.headline)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 20)
                .padding(.bottom, 6)
            if options.isEmpty {
                Text("No \(kind.title.lowercased()) tracks")
                    .padding(20)
            }
            ForEach(options) { option in
                Button { onSelect(option) } label: { MpvTrackRow(option: option) }
                    .buttonStyle(MpvMenuRowStyle())
                    .focused(focus, equals: .option(option.id))
            }
        }
        .padding(18)
        .frame(width: 620, alignment: .leading)
        .glassPanel(cornerRadius: 34)
        .focusSection()
    }
}

private struct MpvTrackRow: View {
    let option: MpvTrackOption

    var body: some View {
        HStack(spacing: 18) {
            Image(systemName: "checkmark")
                .font(.callout.weight(.bold))
                .opacity(option.isSelected ? 1 : 0)
            VStack(alignment: .leading, spacing: 2) {
                Text(option.label)
                    .font(.callout.weight(.semibold))
                if let detail = option.detail {
                    Text(detail)
                        .font(.caption)
                        .opacity(0.7)
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
            .padding(.horizontal, 20)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(.white.opacity(isFocused ? 0.92 : 0))
            )
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeOut(duration: 0.15), value: isFocused)
    }
}

struct MpvInfoPanel: View {
    let title: String
    let subtitle: String?
    let source: String
    let rows: [MpvInfoRow]

    var body: some View {
        HStack(alignment: .top, spacing: 60) {
            VStack(alignment: .leading, spacing: 10) {
                Text(title)
                    .font(.title3.bold())
                    .lineLimit(2)
                if let subtitle {
                    Text(subtitle)
                        .font(.callout)
                        .foregroundStyle(.secondary)
                }
                Text(source)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            .frame(width: 560, alignment: .leading)
            Grid(alignment: .leading, horizontalSpacing: 28, verticalSpacing: 10) {
                ForEach(rows) { row in
                    GridRow {
                        Text(row.label)
                            .foregroundStyle(.secondary)
                        Text(row.value)
                    }
                    .font(.callout)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(36)
        .glassPanel(cornerRadius: 34)
    }
}
