import SwiftUI
import UIKit

final class MpvMetalLayer: CAMetalLayer {
    override var drawableSize: CGSize {
        get { super.drawableSize }
        set {
            guard Int(newValue.width) > 1, Int(newValue.height) > 1 else { return }
            super.drawableSize = newValue
        }
    }
}

final class MpvVideoUIView: UIView {
    private let videoLayer: CAMetalLayer

    init(videoLayer: CAMetalLayer) {
        self.videoLayer = videoLayer
        super.init(frame: .zero)
        backgroundColor = .black
        layer.addSublayer(videoLayer)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        videoLayer.frame = bounds
    }
}

struct MpvVideoView: UIViewRepresentable {
    let videoLayer: CAMetalLayer

    func makeUIView(context: Context) -> MpvVideoUIView {
        MpvVideoUIView(videoLayer: videoLayer)
    }

    func updateUIView(_ view: MpvVideoUIView, context: Context) {}
}
