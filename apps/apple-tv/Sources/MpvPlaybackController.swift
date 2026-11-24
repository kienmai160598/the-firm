import AVFoundation
import NuvioTvShared
import UIKit
import os

@MainActor
final class MpvPlaybackController: ObservableObject {
    private static let tickInterval: Duration = .seconds(10)
    private static let startupLimit: Duration = .seconds(60)
    private static let videoFormatLimit: Duration = .seconds(2)
    private static let videoFormatPoll: Duration = .milliseconds(100)
    private static let seekStepSeconds = 10
    private static let minimumTickAdvanceMs: Int64 = 1000
    private static let endToleranceMs: Int64 = 5000
    private static let log = Logger(subsystem: "com.kien.movies.tvos", category: "Mpv")
    private static let notStarted = "The stream did not start within 60 seconds."
    private static let cannotCreate = "The video player could not be created."
    private static let stoppedEarly = "The stream stopped sending data before the end."

    @Published private(set) var isPaused = true
    @Published private(set) var isPausedByViewer = false
    @Published private(set) var isBuffering = true
    @Published private(set) var hasPlayed = false

    let clock: MpvPlaybackClock
    let scrubber: MpvTouchScrubber
    let videoLayer: CAMetalLayer = MpvMetalLayer()
    private let request: PlaybackRequest
    private let onFailure: (StreamNotice) -> Void
    private let session: TvosPlaybackSession
    private var mpv: MpvHandle?
    private var tickTask: Task<Void, Never>?
    private var startupTask: Task<Void, Never>?
    private var startTask: Task<Void, Never>?
    private var subtitleSearch: TvosCancellable?
    private var subtitleDownloads: [MpvSubtitleDownload] = []
    private let displayMatcher = MpvDisplayMatcher()
    private var lifecycleObservers: [NSObjectProtocol] = []
    private var currentMs: Int64 = 0
    private var durationMs: Int64 = 0
    private var hasStarted = false
    private var hasRequestedPlay = false
    private var hasEnded = false
    private var isFinished = false
    private var lastTickSaveMs: Int64 = -1

    init(request: PlaybackRequest, onFailure: @escaping (StreamNotice) -> Void) {
        self.request = request
        self.onFailure = onFailure
        let clock = MpvPlaybackClock()
        self.clock = clock
        scrubber = MpvTouchScrubber(clock: clock)
        session = TvosPlayback.shared.startSession(target: request.target, stream: request.stream)
        videoLayer.contentsScale = UIScreen.main.nativeScale
        videoLayer.framebufferOnly = true
        videoLayer.backgroundColor = UIColor.black.cgColor
        videoLayer.frame = UIScreen.main.bounds
    }

    func start() {
        guard mpv == nil, !isFinished else { return }
        PlaybackAudioSession.activate()
        let options = MpvOptions.playback(
            headers: request.stream.requestHeaders,
            languages: TvosSettings.shared.playerLanguages(deviceLanguage: LanguageName.device)
        )
        mpv = MpvHandle(layer: videoLayer, options: options) { [weak self] event in
            self?.handle(event)
        }
        guard let mpv else {
            fail(Self.cannotCreate)
            return
        }
        observeProperties(mpv)
        observeLifecycle()
        searchOnlineSubtitles()
        UIApplication.shared.isIdleTimerDisabled = true
        startupTask = Self.after(Self.startupLimit) { [weak self] in self?.giveUpStarting() }
        tickTask = Self.every(Self.tickInterval) { [weak self] in self?.saveTick() }
        Self.log.notice("Loading \(self.request.url.lastPathComponent, privacy: .public)")
        mpv.command("loadfile", request.url.absoluteString, "replace")
    }

    func finish() {
        guard !isFinished else { return }
        save(syncRemote: true, reason: "dismiss")
        isFinished = true
        UIApplication.shared.isIdleTimerDisabled = false
        [tickTask, startupTask, startTask].forEach { $0?.cancel() }
        subtitleSearch?.cancel()
        displayMatcher.reset()
        scrubber.stop()
        lifecycleObservers.forEach(NotificationCenter.default.removeObserver)
        lifecycleObservers = []
        mpv?.close()
        mpv = nil
    }

    func togglePause() {
        setPaused(!isPaused)
    }

    func setPaused(_ isPausing: Bool) {
        guard hasStarted else { return }
        mpv?.setFlag(MpvProperty.pause, isPausing)
    }

    func seek(bySteps steps: Int) {
        guard hasStarted else { return }
        mpv?.command("seek", String(steps * Self.seekStepSeconds), "relative")
    }

    func seek(toSeconds seconds: Int) {
        guard hasStarted else { return }
        mpv?.command("seek", String(seconds), "absolute")
        clock.setPosition(seconds)
    }

    func liftSubtitles(_ isLifted: Bool) {
        mpv?.setString(MpvProperty.subtitlePosition, isLifted ? "66" : "100")
    }

    func trackOptions(_ kind: MpvTrackKind) -> [MpvTrackOption] {
        guard hasStarted, let mpv else { return [] }
        return kind.options(using: mpv, downloads: subtitleDownloads)
    }

    func select(_ option: MpvTrackOption, of kind: MpvTrackKind) {
        guard hasStarted else { return }
        if let download = option.download {
            load(download)
        } else {
            mpv?.setString(kind.selector, option.id)
        }
    }

    private func load(_ download: MpvSubtitleDownload) {
        Self.log.notice("Adding online subtitle \(download.trackTitle, privacy: .public)")
        mpv?.commandInBackground(
            "sub-add",
            download.url,
            "select",
            download.trackTitle,
            download.language
        )
    }

    private func searchOnlineSubtitles() {
        subtitleSearch = TvosSubtitles.shared.load(
            target: request.target,
            stream: request.stream
        ) { [weak self] subtitles in
            self?.subtitleDownloads = MpvSubtitleDownload.inSourceOrder(subtitles)
        }
    }

    func mediaInfo() -> [MpvInfoRow] {
        guard hasStarted, let mpv else { return [] }
        return MpvMediaInfo.rows(using: mpv)
    }

    private func observeProperties(_ mpv: MpvHandle) {
        mpv.observeNumber(MpvProperty.timePosition)
        mpv.observeNumber(MpvProperty.duration)
        mpv.observeNumber(MpvProperty.cacheTime)
        mpv.observeFlag(MpvProperty.pause)
        mpv.observeFlag(MpvProperty.pausedForCache)
        mpv.observeFlag(MpvProperty.endReached)
    }

    private func handle(_ event: MpvEvent) {
        guard !isFinished else { return }
        switch event {
        case .fileLoaded:
            startOnce()
        case .endOfFile:
            updateEnd(true)
        case let .failed(reason):
            fail("The player stopped with an error: \(reason).")
        case let .number(name, value):
            updateNumber(name, value)
        case let .flag(name, value):
            updateFlag(name, value)
        }
    }

    private func startOnce() {
        guard !hasStarted, let mpv else { return }
        hasStarted = true
        durationMs = Self.milliseconds(mpv.number(MpvProperty.duration) ?? 0)
        let startMs = startPositionMs()
        Self.log.notice("Start position \(startMs, privacy: .public) ms of \(self.durationMs)")
        if startMs > 0 {
            mpv.command("seek", String(Double(startMs) / 1000), "absolute+exact")
        }
        startTask = Task { [weak self] in await self?.matchDisplayThenPlay() }
    }

    private func matchDisplayThenPlay() async {
        guard let format = await firstVideoFormat() else { return play() }
        Self.log.notice(
            """
            Video \(format.width, privacy: .public)x\(format.height, privacy: .public) \
            \(format.framesPerSecond, privacy: .public) fps \
            hdr=\(format.dynamicRange.isHighDynamicRange, privacy: .public)
            """
        )
        await displayMatcher.match(format)
        if format.dynamicRange.isHighDynamicRange, displayMatcher.canShowHighDynamicRange {
            mpv?.setString(MpvProperty.colorspaceHint, "yes")
        }
        play()
    }

    private func firstVideoFormat() async -> MpvVideoFormat? {
        let deadline = ContinuousClock.now + Self.videoFormatLimit
        while !isFinished, ContinuousClock.now < deadline {
            if let mpv, let format = MpvVideoFormat.read(from: mpv) { return format }
            try? await Task.sleep(for: Self.videoFormatPoll)
        }
        return nil
    }

    private func play() {
        guard !isFinished else { return }
        hasRequestedPlay = true
        // The audio output set its own session up while the file loaded; make it non-mixable again.
        PlaybackAudioSession.activate()
        Self.log.notice("Audio output \(self.mpv?.string("current-ao") ?? "none", privacy: .public)")
        mpv?.setFlag(MpvProperty.pause, false)
    }

    private func startPositionMs() -> Int64 {
        #if DEBUG
        if let secondsBeforeEnd = DebugLaunch.startSecondsBeforeEnd {
            return max(durationMs - secondsBeforeEnd * 1000, 0)
        }
        #endif
        guard request.resumes, durationMs > 0 else { return 0 }
        return TvosPlayback.shared.resumePositionMs(
            target: request.target,
            actualDurationMs: durationMs
        )
    }

    private func updateNumber(_ name: String, _ value: Double) {
        switch name {
        case MpvProperty.timePosition:
            currentMs = Self.milliseconds(value)
            clock.setPosition(Int(currentMs / 1000))
            markPlayedIfMoving()
        case MpvProperty.duration:
            durationMs = Self.milliseconds(value)
            clock.setDuration(Int(durationMs / 1000))
        case MpvProperty.cacheTime:
            clock.setBuffered(Int(value.isFinite ? max(value, 0) : 0))
        default:
            break
        }
    }

    private func updateFlag(_ name: String, _ value: Bool) {
        switch name {
        case MpvProperty.pause:
            publish(\.isPaused, value)
            publish(\.isPausedByViewer, value && hasRequestedPlay)
            if value { save(syncRemote: true, reason: "pause") }
        case MpvProperty.pausedForCache:
            publish(\.isBuffering, value)
            markPlayedIfMoving()
        case MpvProperty.endReached:
            updateEnd(value)
        default:
            break
        }
    }

    private func markPlayedIfMoving() {
        guard hasStarted, !hasPlayed, !isPaused, !isBuffering, currentMs > 0 else { return }
        hasPlayed = true
        startupTask?.cancel()
        Self.log.notice("Playing at \(self.currentMs, privacy: .public) ms")
    }

    private func giveUpStarting() {
        guard !hasPlayed else { return }
        fail(Self.notStarted)
    }

    private func updateEnd(_ isAtEnd: Bool) {
        guard isAtEnd != hasEnded else { return }
        guard !isAtEnd || isNearEnd else {
            fail(Self.stoppedEarly)
            return
        }
        hasEnded = isAtEnd
        if isAtEnd { save(syncRemote: true, reason: "end") }
    }

    private func fail(_ message: String) {
        guard !isFinished else { return }
        Self.log.error("Playback failed: \(message, privacy: .public) started=\(self.hasStarted)")
        finish()
        onFailure(StreamNotice(title: "Playback failed", message: message))
    }

    private func saveTick() {
        guard abs(currentMs - lastTickSaveMs) >= Self.minimumTickAdvanceMs else { return }
        lastTickSaveMs = currentMs
        save(syncRemote: false, reason: "tick")
    }

    private func save(syncRemote: Bool, reason: String) {
        guard hasPlayed, !isFinished else { return }
        let positionMs = hasEnded ? durationMs : currentMs
        let saved = session.save(
            positionMs: positionMs,
            durationMs: durationMs,
            isPlaying: !isPaused && !hasEnded,
            isEnded: hasEnded,
            syncRemote: syncRemote
        )
        Self.log.notice(
            """
            \(reason, privacy: .public) position=\(positionMs, privacy: .public) ms \
            duration=\(self.durationMs, privacy: .public) ms saved=\(saved, privacy: .public)
            """
        )
    }

    private func observeLifecycle() {
        let center = NotificationCenter.default
        let background = center.addObserver(
            forName: UIApplication.didEnterBackgroundNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in self?.enterBackground() }
        }
        let foreground = center.addObserver(
            forName: UIApplication.willEnterForegroundNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in self?.mpv?.setString(MpvProperty.video, "auto") }
        }
        lifecycleObservers = [background, foreground]
    }

    private func enterBackground() {
        mpv?.setFlag(MpvProperty.pause, true)
        mpv?.setString(MpvProperty.video, "no")
    }

    private var isNearEnd: Bool {
        durationMs <= 0 || currentMs >= durationMs - Self.endToleranceMs
    }

    private func publish<Value: Equatable>(
        _ property: ReferenceWritableKeyPath<MpvPlaybackController, Value>,
        _ value: Value
    ) {
        if self[keyPath: property] != value { self[keyPath: property] = value }
    }

    private static func milliseconds(_ seconds: Double) -> Int64 {
        guard seconds.isFinite, seconds > 0 else { return 0 }
        return Int64(seconds * 1000)
    }

    private static func after(
        _ delay: Duration,
        _ action: @escaping @MainActor () -> Void
    ) -> Task<Void, Never> {
        Task { @MainActor in
            try? await Task.sleep(for: delay)
            guard !Task.isCancelled else { return }
            action()
        }
    }

    private static func every(
        _ interval: Duration,
        _ action: @escaping @MainActor () -> Void
    ) -> Task<Void, Never> {
        Task { @MainActor in
            while !Task.isCancelled {
                try? await Task.sleep(for: interval)
                guard !Task.isCancelled else { return }
                action()
            }
        }
    }
}
