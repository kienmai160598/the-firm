import AVFoundation
import NuvioTvShared

enum PlaybackMetadata {
    static func items(target: TvosStreamTarget) -> [AVMetadataItem] {
        let advisory = PlaybackAdvisory(ageRating: target.ageRating, genres: target.genres)
        let optionalItems: [AVMetadataItem?] = [
            target.subtitle.map { item(.iTunesMetadataTrackSubTitle, value: $0) },
            advisory?.ageRating.map { item(.iTunesMetadataContentRating, value: $0) },
            advisory?.genreLine.map { item(.quickTimeMetadataGenre, value: $0) },
        ]
        return [item(.commonIdentifierTitle, value: target.title)] + optionalItems.compactMap { $0 }
    }

    private static func item(_ identifier: AVMetadataIdentifier, value: String) -> AVMetadataItem {
        let metadata = AVMutableMetadataItem()
        metadata.identifier = identifier
        metadata.value = value as NSString
        metadata.extendedLanguageTag = "und"
        return metadata.copy() as! AVMetadataItem
    }
}
