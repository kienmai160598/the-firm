import SwiftUI

/// A language list like the one in Settings: one row per choice, a checkmark on the chosen one.
struct LanguageSettingsView: View {
    let kind: LanguageKind

    @StateObject private var model = LanguageSettingsModel()
    @FocusState private var focus: String?
    private let choices: [String]

    init(kind: LanguageKind) {
        self.kind = kind
        choices = kind.choices
    }

    var body: some View {
        SettingsSplit {
            SettingsPreview {
                SettingsSymbolArt(symbol: kind.page.symbol)
            } caption: {
                Text(kind.page.caption)
                    .foregroundStyle(.secondary)
            }
        } list: {
            ScrollViewReader { proxy in
                List {
                    Section(kind.listTitle) {
                        ForEach(choices, id: \.self) { code in
                            row(code)
                        }
                    }
                }
                // The chosen row can sit far down the lazy list, where `defaultFocus` cannot reach
                // a row that is not built yet: scroll to it first, then focus it.
                .task { focusSelected(with: proxy) }
            }
        }
        .navigationTitle(kind.page.title)
        .onAppear(perform: model.observe)
        .onDisappear(perform: model.stop)
    }

    private func focusSelected(with proxy: ScrollViewProxy) {
        let selected = model.selected(kind)
        proxy.scrollTo(selected, anchor: .center)
        focus = selected
    }

    private func row(_ code: String) -> some View {
        Button { model.select(code, for: kind) } label: {
            LabeledContent(LanguageName.of(code)) {
                if model.selected(kind) == code {
                    Image(systemName: "checkmark")
                        .accessibilityHidden(true)
                }
            }
        }
        .accessibilityLabel(LanguageName.of(code))
        .accessibilityAddTraits(model.selected(kind) == code ? .isSelected : [])
        .accessibilityIdentifier("language-\(code)")
        .focused($focus, equals: code)
    }
}
