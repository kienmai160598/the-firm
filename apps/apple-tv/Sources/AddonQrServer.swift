import CoreImage.CIFilterBuiltins
import Foundation
import SwiftUI

/// While the Addons screen is open, a phone that scans the QR code gets a one-field page on the
/// local network and sends an addon manifest URL to the TV. A random token in the URL keeps other
/// devices on the network from adding addons.
@MainActor
final class AddonQrServer: ObservableObject {
    @Published private(set) var pageUrl: URL?

    private let token = AddonQrServer.makeToken()
    private let install: (String, @escaping (AddonInstallStatus) -> Void) -> Void
    private lazy var server = LocalHttpServer { [weak self] request, respond in
        self?.handle(request, respond: respond)
    }

    init(install: @escaping (String, @escaping (AddonInstallStatus) -> Void) -> Void) {
        self.install = install
    }

    func start() {
        server.start { [weak self] port in
            guard let self, let port, let host = LocalNetworkAddress.current() else {
                self?.pageUrl = nil
                return
            }
            pageUrl = URL(string: "http://\(host):\(port)/?t=\(token)")
        }
    }

    func stop() {
        server.stop()
        pageUrl = nil
    }

    private func handle(_ request: LocalHttpRequest, respond: @escaping (LocalHttpResponse) -> Void) {
        guard request.query["t"] == token else {
            respond(.text(403, "This link expired. Scan the code on the TV again."))
            return
        }
        switch (request.method, request.path) {
        case ("GET", "/"):
            respond(LocalHttpResponse(status: 200, contentType: "text/html; charset=utf-8",
                                      body: Data(AddonQrPage.html.utf8)))
        case ("POST", "/add"):
            add(request.form["url"] ?? "", respond: respond)
        default:
            respond(.text(404, "Not found"))
        }
    }

    private func add(_ url: String, respond: @escaping (LocalHttpResponse) -> Void) {
        install(url) { status in
            switch status {
            case let .installed(name):
                respond(.text(200, "Added \(name) to the TV."))
            case let .failed(message):
                respond(.text(422, message))
            case .installing:
                respond(.text(409, "The TV is adding another addon. Try again in a moment."))
            }
        }
    }

    private static func makeToken() -> String {
        let letters = Array("abcdefghijkmnpqrstuvwxyz23456789")
        return String((0..<12).map { _ in letters.randomElement()! })
    }
}

enum QrCodeImage {
    /// A sharp QR code for `text`, or nil when it does not fit a code.
    static func make(_ text: String) -> CGImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(text.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage else { return nil }
        let scaled = output.transformed(by: CGAffineTransform(scaleX: 12, y: 12))
        return CIContext().createCGImage(scaled, from: scaled.extent)
    }
}

private enum AddonQrPage {
    static let html = """
    <!doctype html>
    <html lang="en">
    <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Add an addon to Movies</title>
    <style>
    body { font: 17px -apple-system, system-ui, sans-serif; margin: 0; padding: 32px 20px;
           background: #F4F3EF; color: #2B2B30; }
    main { max-width: 480px; margin: 0 auto; }
    h1 { font-size: 26px; margin: 0 0 8px; }
    p { color: #2B2B30A6; margin: 0 0 24px; }
    input { box-sizing: border-box; width: 100%; font: inherit; padding: 14px;
            border: 1px solid #2B2B3040; border-radius: 12px; background: #fff; }
    button { width: 100%; margin-top: 12px; font: inherit; font-weight: 600; padding: 14px;
             border: 0; border-radius: 12px; background: #F26A21; color: #fff; }
    button:disabled { opacity: .5; }
    #result { margin-top: 20px; }
    .failed { color: #C0392B; }
    </style>
    </head>
    <body>
    <main>
    <h1>Add an addon</h1>
    <p>Paste the addon's manifest URL. The TV adds it right away.</p>
    <form id="form">
    <input id="url" name="url" type="url" inputmode="url" autocomplete="off"
           placeholder="https://example.com/manifest.json" required>
    <button id="send" type="submit">Add to TV</button>
    </form>
    <p id="result" role="status"></p>
    </main>
    <script>
    const form = document.getElementById("form");
    const send = document.getElementById("send");
    const result = document.getElementById("result");
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      send.disabled = true;
      result.className = "";
      result.textContent = "Adding...";
      try {
        const response = await fetch("/add" + location.search, {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: new URLSearchParams({ url: document.getElementById("url").value }),
        });
        result.textContent = await response.text();
        if (response.ok) { form.reset(); } else { result.className = "failed"; }
      } catch {
        result.className = "failed";
        result.textContent = "The TV did not answer. Keep the Addons screen open and try again.";
      }
      send.disabled = false;
    });
    </script>
    </body>
    </html>
    """
}
