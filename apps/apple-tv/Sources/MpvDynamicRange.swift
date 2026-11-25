import CoreMedia

/// The transfer function of the video mpv plays, which decides between an SDR and an HDR display
/// mode.
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

    /// The dynamic range, or nil while it is not known yet.
    ///
    /// The container gives the picture size before any frame is decoded, but the transfer function
    /// comes with the first decoded frame (`video-dec-params`): an HDR film read at the size alone
    /// was matched as SDR. A Dolby Vision track is known from the demuxer already, and mpv maps its
    /// frames to PQ, so it does not wait for a frame.
    static func resolve(decodedGamma: String?, dolbyVisionProfile: String?) -> MpvDynamicRange? {
        if let decodedGamma { return MpvDynamicRange(mpvGamma: decodedGamma) }
        return dolbyVisionProfile == nil ? nil : .perceptualQuantizer
    }

    static func read(from mpv: MpvHandle) -> MpvDynamicRange? {
        resolve(
            decodedGamma: mpv.string(MpvProperty.decodedGamma),
            dolbyVisionProfile: mpv.string(MpvProperty.dolbyVisionProfile)
        )
    }

    var isHighDynamicRange: Bool {
        self != .standard
    }

    var transferFunction: CFString? {
        switch self {
        case .standard: nil
        case .perceptualQuantizer: kCMFormatDescriptionTransferFunction_SMPTE_ST_2084_PQ
        case .hybridLogGamma: kCMFormatDescriptionTransferFunction_ITU_R_2100_HLG
        }
    }
}
