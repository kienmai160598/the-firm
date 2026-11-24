import AVFoundation
import NuvioTvShared

/// Hands Settings' languages to AVPlayer's automatic media selection. With subtitles off AVPlayer
/// keeps its own default: none, or what the system captions settings ask for.
enum PlayerLanguageSelection {
    static func apply(_ languages: TvosPlayerLanguages, to player: AVPlayer) {
        player.appliesMediaSelectionCriteriaAutomatically = true
        if !languages.audio.isEmpty {
            player.setMediaSelectionCriteria(criteria(languages.audio), forMediaCharacteristic: .audible)
        }
        if languages.showsSubtitles {
            player.setMediaSelectionCriteria(criteria(languages.subtitles), forMediaCharacteristic: .legible)
        }
    }

    private static func criteria(_ codes: [String]) -> AVPlayerMediaSelectionCriteria {
        AVPlayerMediaSelectionCriteria(preferredLanguages: codes, preferredMediaCharacteristics: nil)
    }
}
