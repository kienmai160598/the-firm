import AVFoundation
import Combine
import NuvioTvShared
import os
import UIKit

/// Turns a backdrop (Home's focused spotlight, a Details title) into its trailer, with sound,
/// after a short wait, and back when focus moves on, the screen goes or the trailer ends. The link
/// is looked up and the player buffers during the wait. It plays hidden for a moment first, while
/// its black bars are measured, so it never shows them.
@MainActor
final class HeroTrailer: ObservableObject {
    static let startDelay: Duration = .seconds(1)
    static let fadeInDuration = 1.2
    static let fadeOutDuration = 0.6
    private static let log = Logger(subsystem: "com.kien.movies.tvos", category: "HeroTrailer")

    /// The title the player belongs to.
    @Published private(set) var itemId: String?
    @Published private(set) var player: AVPlayer?
    /// True once the trailer plays and its crop is known, so the backdrop never fades to a black
    /// frame or to black bars.
    @Published private(set) var isShowing = false
    /// Above 1 for a letterboxed trailer, so its black bars fall outside the screen.
    @Published private(set) var zoom: CGFloat = 1

    private var focusedId: String?
    private var isDue = false
    private var waitTask: Task<Void, Never>?
    private var fadeInTask: Task<Void, Never>?
    private var resolution: TvosCancellable?
    private var source: TrailerPlayerItem?
    private var letterbox: HeroLetterbox?
    private var observers: Set<AnyCancellable> = []

    func picture(for itemId: String) -> HeroTrailerPicture? {
        guard self.itemId == itemId, let player else { return nil }
        return HeroTrailerPicture(player: player, isShowing: isShowing, zoom: zoom)
    }

    /// Looks the links up ahead of focus, so a focused item does not wait for YouTube.
    func prefetch(_ items: [TvosItem]) {
        guard UIAccessibility.isVideoAutoplayEnabled else { return }
        for item in items {
            TvosTrailers.shared.prefetch(type: item.type, id: item.metaId)
        }
    }

    func focus(_ item: TvosItem?) {
        item.map { play(type: $0.type, id: $0.metaId) } ?? stop()
    }

    func play(type: String, id: String) {
        guard id != focusedId else { return }
        stop()
        guard UIAccessibility.isVideoAutoplayEnabled else { return }
        focusedId = id
        resolution = TvosTrailers.shared.resolve(type: type, id: id) { [weak self] link in
            self?.prepare(link, for: id)
        }
        waitTask = Task { [weak self] in
            try? await Task.sleep(for: Self.startDelay)
            guard !Task.isCancelled, let self else { return }
            isDue = true
            player?.play()
        }
    }

    func stop() {
        focusedId = nil
        isDue = false
        waitTask?.cancel()
        waitTask = nil
        resolution?.cancel()
        resolution = nil
        fadeOut()
    }

    private func prepare(_ link: TvosTrailerLink?, for id: String) {
        guard focusedId == id, let link, let source = TrailerPlayerItem(link: link) else { return }
        let playerItem = source.item
        let player = AVPlayer(playerItem: playerItem)
        player.volume = 0
        player.preventsDisplaySleepDuringVideoPlayback = false
        observe(player, playing: playerItem)
        zoom = 1
        itemId = id
        self.source = source
        self.player = player
        if isDue { player.play() }
    }

    private func observe(_ player: AVPlayer, playing item: AVPlayerItem) {
        player.publisher(for: \.timeControlStatus)
            .first { $0 == .playing }
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in self?.measureLetterbox(of: item, then: player) }
            .store(in: &observers)
        item.publisher(for: \.status)
            .first { $0 == .failed }
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in self?.fail(item) }
            .store(in: &observers)
        let center = NotificationCenter.default
        center.publisher(for: AVPlayerItem.didPlayToEndTimeNotification, object: item)
            .merge(with: center.publisher(for: AVPlayerItem.failedToPlayToEndTimeNotification, object: item))
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in self?.fadeOut() }
            .store(in: &observers)
    }

    private func measureLetterbox(of item: AVPlayerItem, then player: AVPlayer) {
        letterbox = HeroLetterbox(item: item) { [weak self] zoom in
            self?.zoom = zoom
            self?.fadeIn(player)
        }
    }

    private func fadeIn(_ player: AVPlayer) {
        let size = player.currentItem?.presentationSize ?? .zero
        Self.log.notice("Trailer shows at \(String(describing: size), privacy: .public)")
        isShowing = true
        fadeInTask = Task { await TrailerVolume.ramp(player, to: 1, over: Self.fadeInDuration) }
    }

    private func fail(_ item: AVPlayerItem) {
        let reason = item.error?.localizedDescription ?? "unknown"
        Self.log.error("Trailer failed to load: \(reason, privacy: .public)")
        fadeOut()
    }

    /// Fades picture and sound out together, then lets the player go.
    private func fadeOut() {
        observers.removeAll()
        fadeInTask?.cancel()
        fadeInTask = nil
        letterbox?.stop()
        letterbox = nil
        isShowing = false
        guard let fading = player else { return }
        Task { [weak self] in
            await TrailerVolume.ramp(fading, to: 0, over: Self.fadeOutDuration)
            fading.pause()
            guard let self, player === fading else { return }
            player = nil
            source = nil
            itemId = nil
        }
    }
}
