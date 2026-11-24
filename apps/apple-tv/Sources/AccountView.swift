import NuvioTvShared
import SwiftUI

/// Trakt sign in, laid out like the Settings pages: the code and its QR on the left while the
/// user approves it on a phone, the account actions on the right.
struct AccountView: View {
    @StateObject private var model = TraktAccountModel()
    @State private var confirmsSignOut = false

    var body: some View {
        SettingsSplit {
            SettingsPreview {
                art
            } caption: {
                Text(caption)
                    .foregroundStyle(.secondary)
            }
        } list: {
            List {
                Section {
                    rows
                } header: {
                    Text("Trakt")
                } footer: {
                    if let footer { Text(footer) }
                }
            }
        }
        .navigationTitle(SettingsPage.account.title)
        .confirmationDialog("Sign out of Trakt?", isPresented: $confirmsSignOut) {
            Button("Sign Out", role: .destructive, action: model.signOut)
        }
        .onAppear(perform: model.observe)
        .onDisappear(perform: model.stop)
    }

    @ViewBuilder
    private var rows: some View {
        switch model.state.phase {
        case .signedIn:
            LabeledContent("Account", value: model.summary)
            Button("Sign Out") { confirmsSignOut = true }
                .accessibilityIdentifier("trakt-sign-out")
        case .waitingForApproval:
            LabeledContent("Code", value: model.state.userCode ?? "")
                .accessibilityIdentifier("trakt-code")
            Button("Cancel", action: model.cancel)
        case .starting:
            LabeledContent("Getting a Code") { ProgressView() }
        case .notConfigured:
            // Still a button, so the page holds focus; it has nothing to do.
            Button {} label: {
                LabeledContent("Sign In to Trakt", value: model.summary)
            }
            .accessibilityLabel("Sign In to Trakt, \(model.summary)")
            .accessibilityIdentifier("trakt-sign-in")
        default:
            Button("Sign In to Trakt", action: model.signIn)
                .accessibilityIdentifier("trakt-sign-in")
        }
    }

    @ViewBuilder
    private var art: some View {
        if model.state.phase == .waitingForApproval, let url = model.state.verificationUrl {
            TraktCodeArt(code: model.state.userCode ?? "", url: url)
        } else {
            SettingsSymbolArt(symbol: SettingsPage.account.symbol)
        }
    }

    private var caption: String {
        model.state.phase == .waitingForApproval
            ? "Scan the code or open the address on a phone, then type the code there."
            : SettingsPage.account.caption
    }

    private var footer: String? {
        model.state.phase == .notConfigured
            ? "This build has no Trakt client id. Add TRAKT_CLIENT_ID and TRAKT_CLIENT_SECRET to shared/local.properties and build again."
            : model.failureText
    }
}

/// The address and code to approve, with a QR code of the address.
private struct TraktCodeArt: View {
    let code: String
    let url: String

    var body: some View {
        VStack(spacing: 32) {
            if let image = QrCodeImage.make(url) {
                Image(decorative: image, scale: 1)
                    .interpolation(.none)
                    .resizable()
                    .frame(width: 260, height: 260)
            }
            Text(url.replacingOccurrences(of: "https://", with: ""))
                .font(.headline)
            Text(code)
                .font(.system(size: 72, weight: .bold, design: .monospaced))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(.quaternary, in: RoundedRectangle(cornerRadius: SettingsMetrics.artRadius, style: .continuous))
    }
}
