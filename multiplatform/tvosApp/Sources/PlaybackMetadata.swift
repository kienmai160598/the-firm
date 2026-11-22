import AVFoundation
import NuvioTvShared

enum PlaybackMetadata {
    static func items(target: TvosStreamTarget, stream: TvosStream) -> [AVMetadataItem] {
        let subtitle = target.subtitle
            ?? [stream.name, stream.addonName].joined(separator: "  ·  ")
        return [
            item(.commonIdentifierTitle, value: target.title),
            item(.iTunesMetadataTrackSubTitle, value: subtitle),
        ]
    }

    private static func item(_ identifier: AVMetadataIdentifier, value: String) -> AVMetadataItem {
        let metadata = AVMutableMetadataItem()
        metadata.identifier = identifier
        metadata.value = value as NSString
        metadata.extendedLanguageTag = "und"
        return metadata.copy() as! AVMetadataItem
    }
}
