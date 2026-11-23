import AVFoundation
import NuvioTvShared

/// Samples small frames from the start of a trailer and reports, once, the zoom that crops its
/// black bars: above 1 for a letterboxed trailer, 1 when no bars held still within the sample
/// limit. The detection itself is Kotlin.
@MainActor
final class HeroLetterbox {
    private static let bytesPerPixel = 4
    private static let opaque: UInt32 = 0xFF00_0000

    private let detector = TvosLetterbox()
    private let output: AVPlayerItemVideoOutput
    private var task: Task<Void, Never>?

    init(item: AVPlayerItem, onSettled: @escaping (CGFloat) -> Void) {
        output = AVPlayerItemVideoOutput(pixelBufferAttributes: [
            kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA,
        ])
        item.add(output)
        task = Task { [weak self, weak item] in
            guard let item else { return }
            await self?.sample(item, onSettled: onSettled)
        }
    }

    func stop() {
        task?.cancel()
        task = nil
    }

    private func sample(_ item: AVPlayerItem, onSettled: (CGFloat) -> Void) async {
        let interval = Duration.milliseconds(detector.sampleIntervalMs)
        for _ in 0..<Int(detector.sampleLimit) {
            try? await Task.sleep(for: interval)
            guard !Task.isCancelled else { return }
            guard let pixels = framePixels(at: item.currentTime()) else { continue }
            if let zoom = detector.onFrame(argbPixels: pixels) {
                onSettled(CGFloat(zoom.floatValue))
                return
            }
        }
        onSettled(1)
    }

    /// The frame at `time`, scaled down to the detector's sample size by picking pixels.
    private func framePixels(at time: CMTime) -> KotlinIntArray? {
        guard output.hasNewPixelBuffer(forItemTime: time),
              let buffer = output.copyPixelBuffer(forItemTime: time, itemTimeForDisplay: nil)
        else { return nil }
        CVPixelBufferLockBaseAddress(buffer, .readOnly)
        defer { CVPixelBufferUnlockBaseAddress(buffer, .readOnly) }
        guard let base = CVPixelBufferGetBaseAddress(buffer) else { return nil }
        let bytes = base.assumingMemoryBound(to: UInt8.self)
        let width = CVPixelBufferGetWidth(buffer)
        let height = CVPixelBufferGetHeight(buffer)
        let rowBytes = CVPixelBufferGetBytesPerRow(buffer)
        let sampleWidth = Int(detector.sampleWidth)
        let sampleHeight = Int(detector.sampleHeight)
        let pixels = KotlinIntArray(size: Int32(sampleWidth * sampleHeight))
        for row in 0..<sampleHeight {
            let line = bytes + (row * height / sampleHeight) * rowBytes
            for column in 0..<sampleWidth {
                let pixel = line + (column * width / sampleWidth) * Self.bytesPerPixel
                let argb = Self.opaque | UInt32(pixel[2]) << 16 | UInt32(pixel[1]) << 8 | UInt32(pixel[0])
                pixels.set(index: Int32(row * sampleWidth + column), value: Int32(bitPattern: argb))
            }
        }
        return pixels
    }
}
