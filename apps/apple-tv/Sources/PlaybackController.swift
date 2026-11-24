import AVFoundation
import NuvioTvShared
import os

@MainActor
final class PlaybackController {
    private static let tickInterval = CMTime(seconds: 10, preferredTimescale: 600)
    private static let minimumTickAdvanceMs: Int64 = 1000
    private static let headerFieldsKey = "AVURLAssetHTTPHeaderFieldsKey"
    private static let log = Logger(subsystem: "com.kien.movies.tvos", category: "Player")

    let player: AVPlayer
    private let request: PlaybackRequest
    private let onFailure: (StreamNotice) -> Void
    private let session: TvosPlaybackSession
    private var stallWatchdog: StallWatchdog?
    private var startupDeadline: StartupDeadline?
    private var tickObserver: Any?
    private var statusObservation: NSKeyValueObservation?
    private var controlObservation: NSKeyValueObservation?
    private var endObserver: NSObjectProtocol?
    private var failureObserver: NSObjectProtocol?
    private var hasStarted = false
    private var hasPlayed = false
    private var hasEnded = false
    private var isFinished = false
    private var lastTickSaveMs: Int64 = -1

    init(request: PlaybackRequest, onFailure: @escaping (StreamNotice) -> Void) {
        self.request = request
        self.onFailure = onFailure
        session = TvosPlayback.shared.startSession(target: request.target, stream: request.stream)
        PlaybackAudioSession.activate()
        let item = AVPlayerItem(asset: Self.asset(for: request))
        item.externalMetadata = PlaybackMetadata.items(target: request.target)
        player = AVPlayer()
        PlayerLanguageSelection.apply(
            TvosSettings.shared.playerLanguages(deviceLanguage: LanguageName.device),
            to: player
        )
        player.replaceCurrentItem(with: item)
        stallWatchdog = StallWatchdog(item: item) { [weak self] error in
            self?.fail(error)
        }
        startupDeadline = StartupDeadline(item: item) { [weak self] error in
            self?.fail(error)
        }
        observe(item)
        startupDeadline?.start()
    }

    func finish() {
        guard !isFinished else { return }
        isFinished = true
        save(syncRemote: true, reason: "dismiss")
        player.pause()
        removeObservers()
    }

    static func asset(for request: PlaybackRequest) -> AVURLAsset {
        let headers = request.stream.requestHeaders
        guard !headers.isEmpty else { return AVURLAsset(url: request.url) }
        return AVURLAsset(url: request.url, options: [headerFieldsKey: headers])
    }

    private func observe(_ item: AVPlayerItem) {
        statusObservation = item.observe(\.status) { [weak self] item, _ in
            Task { @MainActor in self?.handleStatus(item.status) }
        }
        controlObservation = player.observe(\.timeControlStatus) { [weak self] player, _ in
            Task { @MainActor in self?.handleControlStatus(player.timeControlStatus) }
        }
        endObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: item,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in self?.handleEnd() }
        }
        failureObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemFailedToPlayToEndTime,
            object: item,
            queue: .main
        ) { [weak self] notification in
            let error = notification.userInfo?[AVPlayerItemFailedToPlayToEndTimeErrorKey] as? Error
            Task { @MainActor in self?.fail(error) }
        }
        tickObserver = player.addPeriodicTimeObserver(
            forInterval: Self.tickInterval,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in self?.saveTick() }
        }
    }

    private func handleStatus(_ status: AVPlayerItem.Status) {
        switch status {
        case .readyToPlay:
            startOnce()
        case .failed:
            fail(player.currentItem?.error)
        default:
            break
        }
    }

    private func startOnce() {
        guard !hasStarted else { return }
        hasStarted = true
        let startMs = startPositionMs()
        Self.log.notice("Start position \(startMs, privacy: .public) ms")
        guard startMs > 0 else {
            player.play()
            return
        }
        let start = CMTime(value: startMs, timescale: 1000)
        player.seek(to: start, toleranceBefore: .zero, toleranceAfter: .zero) { [weak self] _ in
            Task { @MainActor in self?.player.play() }
        }
    }

    private func startPositionMs() -> Int64 {
        #if DEBUG
        if let secondsBeforeEnd = DebugLaunch.startSecondsBeforeEnd {
            return max(durationMs - secondsBeforeEnd * 1000, 0)
        }
        #endif
        guard request.resumes else { return 0 }
        return TvosPlayback.shared.resumePositionMs(
            target: request.target,
            actualDurationMs: durationMs
        )
    }

    private func fail(_ error: Error?) {
        guard !isFinished else { return }
        let reason = error.map(PlaybackPreflight.readable) ?? "Unknown error."
        Self.log.error("Playback failed: \(reason, privacy: .public) started=\(self.hasStarted)")
        finish()
        onFailure(StreamNotice(
            title: "Playback failed",
            message: "The player stopped with an error: \(reason)"
        ))
    }

    private func handleControlStatus(_ status: AVPlayer.TimeControlStatus) {
        guard hasStarted, !isFinished, !hasEnded else { return }
        updateStartupDeadline(status)
        stallWatchdog?.update(status)
        guard status == .paused else { return }
        save(syncRemote: true, reason: "pause")
    }

    private func updateStartupDeadline(_ status: AVPlayer.TimeControlStatus) {
        switch status {
        case .playing:
            hasPlayed = true
            startupDeadline?.cancel()
        case .paused where !hasPlayed:
            startupDeadline?.cancel()
        case .waitingToPlayAtSpecifiedRate where !hasPlayed:
            startupDeadline?.start()
        default:
            break
        }
    }

    private func handleEnd() {
        hasEnded = true
        save(syncRemote: true, reason: "end")
    }

    private func saveTick() {
        guard abs(currentMs - lastTickSaveMs) >= Self.minimumTickAdvanceMs else { return }
        lastTickSaveMs = currentMs
        save(syncRemote: false, reason: "tick")
    }

    private func save(syncRemote: Bool, reason: String) {
        guard hasStarted, hasPlayed else { return }
        let positionMs = hasEnded ? durationMs : currentMs
        let saved = session.save(
            positionMs: positionMs,
            durationMs: durationMs,
            isPlaying: player.timeControlStatus == .playing,
            isEnded: hasEnded,
            syncRemote: syncRemote
        )
        let duration = durationMs
        Self.log.notice(
            """
            \(reason, privacy: .public) currentTime=\(positionMs, privacy: .public) ms \
            duration=\(duration, privacy: .public) ms saved=\(saved, privacy: .public)
            """
        )
    }

    private var currentMs: Int64 {
        Self.milliseconds(player.currentTime())
    }

    private var durationMs: Int64 {
        Self.milliseconds(player.currentItem?.duration ?? .invalid)
    }

    private static func milliseconds(_ time: CMTime) -> Int64 {
        let seconds = time.seconds
        guard seconds.isFinite, seconds > 0 else { return 0 }
        return Int64(seconds * 1000)
    }

    private func removeObservers() {
        stallWatchdog?.stop()
        startupDeadline?.cancel()
        if let tickObserver {
            player.removeTimeObserver(tickObserver)
        }
        for observer in [endObserver, failureObserver].compactMap({ $0 }) {
            NotificationCenter.default.removeObserver(observer)
        }
        tickObserver = nil
        endObserver = nil
        failureObserver = nil
        statusObservation = nil
        controlObservation = nil
    }
}
