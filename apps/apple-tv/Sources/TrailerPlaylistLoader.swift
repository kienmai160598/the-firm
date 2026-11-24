import AVFoundation
import NuvioTvShared

/// A trailer's player item. For an HLS trailer it plays the master playlist cut down to its
/// sharpest variants (TvosTrailerLink.playlist): AVPlayer asks this loader for the master under a
/// private scheme, and fetches the variants, whose links are absolute https, by itself.
@MainActor
final class TrailerPlayerItem {
    let item: AVPlayerItem
    private let loader: TrailerPlaylistLoader?

    init?(link: TvosTrailerLink) {
        if let playlist = link.playlist {
            let loader = TrailerPlaylistLoader(playlist: playlist)
            item = AVPlayerItem(asset: loader.asset())
            self.loader = loader
        } else {
            guard let url = URL(string: link.url) else { return nil }
            item = AVPlayerItem(url: url)
            loader = nil
        }
    }
}

/// Answers AVPlayer's one request for the rewritten master playlist.
private final class TrailerPlaylistLoader: NSObject, AVAssetResourceLoaderDelegate {
    private static let url = URL(string: "movies-trailer://playlist/master.m3u8")!
    private static let queue = DispatchQueue(label: "com.kien.movies.tvos.trailer-playlist")

    private let playlist: Data

    init(playlist: String) {
        self.playlist = Data(playlist.utf8)
    }

    /// The resource loader holds its delegate weakly, so the caller keeps this loader alive.
    func asset() -> AVURLAsset {
        let asset = AVURLAsset(url: Self.url)
        asset.resourceLoader.setDelegate(self, queue: Self.queue)
        return asset
    }

    func resourceLoader(
        _ resourceLoader: AVAssetResourceLoader,
        shouldWaitForLoadingOfRequestedResource loadingRequest: AVAssetResourceLoadingRequest
    ) -> Bool {
        guard loadingRequest.request.url?.scheme == Self.url.scheme else { return false }
        loadingRequest.dataRequest?.respond(with: playlist)
        loadingRequest.finishLoading()
        return true
    }
}
