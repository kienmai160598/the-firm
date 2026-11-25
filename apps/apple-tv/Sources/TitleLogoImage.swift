import SwiftUI
import UIKit

/// A title logo with its fully transparent margin cropped, so the logo's ink, not its canvas,
/// starts on the column's leading edge where the text and the buttons start (Android
/// TrimTransparentEdges does the same). Shows `placeholder` while loading and when the logo
/// cannot be loaded, as AsyncImage's empty and failure phases do. The logo fits in `maxSize`,
/// on its bottom leading corner.
struct TitleLogoImage<Placeholder: View>: View {
    let url: String?
    let maxSize: CGSize
    @ViewBuilder let placeholder: () -> Placeholder

    @State private var logo: UIImage?

    var body: some View {
        Group {
            if let logo {
                Image(uiImage: logo)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(maxWidth: maxSize.width, maxHeight: maxSize.height, alignment: .bottomLeading)
            } else {
                placeholder()
            }
        }
        .task(id: url) {
            logo = nil
            logo = await TrimmedLogoLoader.shared.logo(at: url.flatMap(URL.init(string:)))
        }
    }
}

/// Loads, trims and keeps the trimmed logos, so a logo is trimmed once per launch.
private actor TrimmedLogoLoader {
    static let shared = TrimmedLogoLoader()

    private let cache = NSCache<NSURL, UIImage>()

    /// The trimmed logo, or nil when there is no URL or the download or decode failed: the
    /// caller then keeps showing the title text.
    func logo(at url: URL?) async -> UIImage? {
        guard let url else { return nil }
        if let cached = cache.object(forKey: url as NSURL) { return cached }
        let data: Data
        do {
            (data, _) = try await URLSession.shared.data(from: url)
        } catch {
            return nil
        }
        guard let image = UIImage(data: data) else { return nil }
        let trimmed = TransparentEdges.trimmed(image)
        cache.setObject(trimmed, forKey: url as NSURL)
        return trimmed
    }
}

enum TransparentEdges {
    /// Alpha at or below this counts as transparent, as on Android.
    static let alphaThreshold: UInt8 = 8

    static func trimmed(_ image: UIImage) -> UIImage {
        guard let source = image.cgImage,
              let ink = inkBounds(of: source),
              ink.size != CGSize(width: source.width, height: source.height),
              let cropped = source.cropping(to: ink)
        else { return image }
        return UIImage(cgImage: cropped, scale: image.scale, orientation: image.imageOrientation)
    }

    /// The smallest pixel rect, top-left origin, holding every pixel above the threshold; nil for
    /// an opaque image or a blank one.
    static func inkBounds(of image: CGImage) -> CGRect? {
        guard hasAlpha(image) else { return nil }
        let width = image.width
        let height = image.height
        var alpha = [UInt8](repeating: 0, count: width * height)
        let drawn = alpha.withUnsafeMutableBytes { buffer -> Bool in
            guard let context = CGContext(
                data: buffer.baseAddress, width: width, height: height, bitsPerComponent: 8,
                bytesPerRow: width, space: CGColorSpaceCreateDeviceGray(),
                bitmapInfo: CGImageAlphaInfo.alphaOnly.rawValue
            ) else { return false }
            context.draw(image, in: CGRect(x: 0, y: 0, width: width, height: height))
            return true
        }
        guard drawn else { return nil }
        var left = width, top = height, right = -1, bottom = -1
        for y in 0..<height {
            let row = y * width
            for x in 0..<width where alpha[row + x] > alphaThreshold {
                left = min(left, x)
                right = max(right, x)
                top = min(top, y)
                bottom = y
            }
        }
        guard right >= 0 else { return nil }
        return CGRect(x: left, y: top, width: right - left + 1, height: bottom - top + 1)
    }

    private static func hasAlpha(_ image: CGImage) -> Bool {
        switch image.alphaInfo {
        case .none, .noneSkipFirst, .noneSkipLast: false
        default: true
        }
    }
}
