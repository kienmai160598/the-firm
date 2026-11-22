import AVFoundation
import AVKit
import CoreMedia
import UIKit

enum MpvDynamicRange: Equatable {
    case standard
    case perceptualQuantizer
    case hybridLogGamma

    init(mpvGamma: String?) {
        switch mpvGamma {
        case "pq": self = .perceptualQuantizer
        case "hlg": self = .hybridLogGamma
        default: self = .standard
        }
    }

    var isHighDynamicRange: Bool {
        self != .standard
    }

    fileprivate var transferFunction: CFString? {
        switch self {
        case .standard: nil
        case .perceptualQuantizer: kCMFormatDescriptionTransferFunction_SMPTE_ST_2084_PQ
        case .hybridLogGamma: kCMFormatDescriptionTransferFunction_ITU_R_2100_HLG
        }
    }
}

struct MpvVideoFormat: Equatable {
    let width: Int32
    let height: Int32
    let framesPerSecond: Double
    let dynamicRange: MpvDynamicRange

    static func read(from mpv: MpvHandle) -> MpvVideoFormat? {
        guard let width = mpv.string("video-params/w").flatMap(Int32.init),
              let height = mpv.string("video-params/h").flatMap(Int32.init)
        else { return nil }
        let framesPerSecond = mpv.string("container-fps").flatMap(Double.init)
            ?? mpv.string("estimated-vf-fps").flatMap(Double.init)
            ?? 0
        return MpvVideoFormat(
            width: width,
            height: height,
            framesPerSecond: framesPerSecond,
            dynamicRange: MpvDynamicRange(mpvGamma: mpv.string("video-params/gamma"))
        )
    }

    var displayCriteria: AVDisplayCriteria? {
        guard framesPerSecond > 0, let description = formatDescription else { return nil }
        return AVDisplayCriteria(
            refreshRate: Float(framesPerSecond),
            formatDescription: description
        )
    }

    private var formatDescription: CMVideoFormatDescription? {
        var description: CMVideoFormatDescription?
        let status = CMVideoFormatDescriptionCreate(
            allocator: kCFAllocatorDefault,
            codecType: dynamicRange.isHighDynamicRange
                ? kCMVideoCodecType_HEVC
                : kCMVideoCodecType_H264,
            width: width,
            height: height,
            extensions: colorExtensions,
            formatDescriptionOut: &description
        )
        return status == noErr ? description : nil
    }

    private var colorExtensions: CFDictionary? {
        guard let transferFunction = dynamicRange.transferFunction else { return nil }
        let extensions: [CFString: CFString] = [
            kCMFormatDescriptionExtension_TransferFunction: transferFunction,
            kCMFormatDescriptionExtension_ColorPrimaries:
                kCMFormatDescriptionColorPrimaries_ITU_R_2020,
            kCMFormatDescriptionExtension_YCbCrMatrix: kCMFormatDescriptionYCbCrMatrix_ITU_R_2020,
        ]
        return extensions as CFDictionary
    }
}

@MainActor
final class MpvDisplayMatcher {
    private static let pollInterval: Duration = .milliseconds(100)
    private static let switchStartGrace: Duration = .milliseconds(400)
    private static let switchLimit: Duration = .seconds(6)

    private var hasRequestedCriteria = false

    var canShowHighDynamicRange: Bool {
        guard let manager = displayManager else { return false }
        return manager.isDisplayCriteriaMatchingEnabled && AVPlayer.eligibleForHDRPlayback
    }

    func match(_ format: MpvVideoFormat) async {
        guard let manager = displayManager,
              manager.isDisplayCriteriaMatchingEnabled,
              let criteria = format.displayCriteria
        else { return }
        manager.preferredDisplayCriteria = criteria
        hasRequestedCriteria = true
        await waitForModeSwitch(on: manager)
    }

    func reset() {
        guard hasRequestedCriteria else { return }
        displayManager?.preferredDisplayCriteria = nil
        hasRequestedCriteria = false
    }

    private func waitForModeSwitch(on manager: AVDisplayManager) async {
        try? await Task.sleep(for: Self.switchStartGrace)
        let deadline = ContinuousClock.now + Self.switchLimit
        while manager.isDisplayModeSwitchInProgress, ContinuousClock.now < deadline {
            try? await Task.sleep(for: Self.pollInterval)
        }
    }

    private var displayManager: AVDisplayManager? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .avDisplayManager
    }
}
