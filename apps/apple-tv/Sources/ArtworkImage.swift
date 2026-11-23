import SwiftUI

struct ArtworkImage: View {
    let url: String?
    var contentMode: ContentMode = .fill
    var fallbackTitle: String?

    var body: some View {
        AsyncImage(url: url.flatMap(URL.init(string:))) { phase in
            if let image = phase.image {
                image.resizable().aspectRatio(contentMode: contentMode)
            } else {
                placeholder(showsTitle: url == nil || phase.error != nil)
            }
        }
    }

    private func placeholder(showsTitle: Bool) -> some View {
        Rectangle()
            .fill(.quaternary)
            .overlay {
                if showsTitle, let fallbackTitle {
                    Text(fallbackTitle)
                        .font(.system(size: 28, weight: .semibold))
                        .multilineTextAlignment(.center)
                        .lineLimit(4)
                        .foregroundStyle(.secondary)
                        .padding(24)
                }
            }
    }
}
