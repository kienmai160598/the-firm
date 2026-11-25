package com.kien.movies.player

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.kien.movies.bridge.BridgeFlows
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.settings.LanguageName
import com.nuvio.tvos.TvosPlayback
import com.nuvio.tvos.TvosPlaybackSession
import com.nuvio.tvos.TvosSettings
import com.nuvio.tvos.rules.OnlineSubtitle
import com.nuvio.tvos.rules.OnlineSubtitleChoice
import com.nuvio.tvos.rules.ProgressSavePolicy
import com.nuvio.tvos.rules.ProgressSavePolicy.Moment
import com.nuvio.tvos.rules.StartingSubtitle
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Plays one [PlaybackRequest] with ExoPlayer (tvOS MpvPlaybackController.swift): start position,
 * state machine, progress saves, startup deadline, stall watchdog, scrub, tracks and online
 * subtitles. Main thread only.
 *
 * Lifetime: [start] once, [finish] once (idempotent). [finish] saves with a remote sync and
 * releases the engine; the screen calls it when the cover leaves (Back, end, failure, Streams
 * popped). It owns its own coroutine scope because the ViewModel that holds it lives as long as
 * the Streams entry, not as long as the cover.
 */
@OptIn(UnstableApi::class)
class PlayerController(private val context: Context, private val request: PlaybackRequest) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val session: TvosPlaybackSession = TvosPlayback.startSession(request.target, request.stream)
    private val loadedBytes = AtomicLong(0)
    private val startupDeadline = StartupDeadline()
    private val stallWatchdog = StallWatchdog()

    private val mutableState = MutableStateFlow(PlayerState())
    private val mutableClock = MutableStateFlow(PlaybackClock())
    private val mutablePreview = MutableStateFlow<Int?>(null)
    private val mutablePlayer = MutableStateFlow<ExoPlayer?>(null)

    val state: StateFlow<PlayerState> = mutableState.asStateFlow()
    val clock: StateFlow<PlaybackClock> = mutableClock.asStateFlow()

    /** The preview position of a pending scrub, in seconds; null when none is pending. */
    val scrubPreview: StateFlow<Int?> = mutablePreview.asStateFlow()

    /** The engine while the playback runs, for the video surface; null before start and after finish. */
    val player: StateFlow<ExoPlayer?> = mutablePlayer.asStateFlow()

    /** "Addon, file name" for the Info panel's Source section. */
    val source: String = listOf(request.stream.addonName, request.stream.filename ?: request.stream.name).joinToString(", ")

    private var onlineSubtitles: List<OnlineSubtitle> = emptyList()
    private val startingSubtitle = StartingSubtitle(TvosSettings.playerLanguages(LanguageName.device).subtitles)
    private val loadedOnline = linkedMapOf<String, MediaItem.SubtitleConfiguration>()
    private var pendingOnlineId: String? = null
    private var pendingAudioFormatId: String? = null
    private var decoders = DecoderNames()
    private var lastTickPositionMs: Long? = null
    private var positionAtLoadMs: Long? = null
    private var isStarted = false
    private var isFinished = false
    private var isInBackground = false
    private var scrub: ScrubSession? = null
    private var resumesAfterScrub = false
    private var settleJob: Job? = null

    val isScrubPending: Boolean get() = scrub != null

    fun start() {
        if (isStarted || isFinished) return
        isStarted = true
        val dataSource = PlaybackDataSource.factory(context, request.url, request.stream.requestHeaders, ByteCounter(loadedBytes))
        val exo = PlayerFactory.create(context, dataSource)
        exo.trackSelectionParameters = PlayerLanguageSelection.applied(exo.trackSelectionParameters, TvosSettings.playerLanguages(LanguageName.device))
        exo.addListener(engineListener)
        exo.addAnalyticsListener(decoderListener)
        positionAtLoadMs = resumePositionAtLoadMs()
        exo.setMediaItem(mediaItem(), positionAtLoadMs ?: C.TIME_UNSET)
        exo.playWhenReady = false
        exo.prepare()
        mutablePlayer.value = exo
        scope.launch { tickEvery(CLOCK_TICK_MS, ::tick) }
        scope.launch { tickEvery(ProgressSavePolicy.TICK_INTERVAL_MS) { save(Moment.Tick) } }
        scope.launch {
            onlineSubtitles = OnlineSubtitleChoice.inSourceOrder(BridgeFlows.subtitles(request.target, request.stream))
            loadStartingSubtitle(startingSubtitle.online(onlineSubtitles))
        }
    }

    /** Saves with a remote sync and releases the engine. Safe to call more than once. */
    fun finish() {
        if (isFinished) return
        // An ended playback was saved as ended already; a second save would only sync again.
        if (mutableState.value.phase != PlaybackPhase.Ended) save(Moment.Exit)
        isFinished = true
        scope.cancel()
        val exo = mutablePlayer.value
        mutablePlayer.value = null
        exo?.removeListener(engineListener)
        exo?.removeAnalyticsListener(decoderListener)
        exo?.release()
    }

    fun togglePause() = setPaused(!mutableState.value.isPaused)

    fun setPaused(isPausing: Boolean) {
        if (!canCommand) return
        mutablePlayer.value?.playWhenReady = !isPausing
    }

    /** Skips [steps] times [HoldScrub.TAP_STEP_SECONDS], back when negative. */
    fun skip(steps: Int) {
        val exo = mutablePlayer.value ?: return
        if (!canCommand) return
        val target = (exo.currentPosition + steps * HoldScrub.TAP_STEP_SECONDS * MILLIS_PER_SECOND).coerceAtLeast(0)
        seekToKeyframe(exo, durationMs(exo).takeIf { it > 0 }?.let { target.coerceAtMost(it) } ?: target, isForward = steps > 0)
        publishClock(exo)
    }

    /** Starts a pending scrub from the current position and pauses; false when the length is unknown. */
    fun beginScrub(): Boolean {
        if (scrub != null) return true
        val clockNow = mutableClock.value
        if (!canCommand || clockNow.durationSeconds <= 0) return false
        resumesAfterScrub = !mutableState.value.isPaused
        setPaused(true)
        scrub = ScrubSession(originSeconds = clockNow.positionSeconds, durationSeconds = clockNow.durationSeconds)
        mutablePreview.value = clockNow.positionSeconds
        return true
    }

    /** Moves the pending preview and seeks there [HoldScrub.SETTLE_MILLIS] after the last move. */
    fun moveScrub(bySeconds: Double) {
        val moved = scrub?.moved(bySeconds) ?: return
        scrub = moved
        mutablePreview.value = moved.shownSeconds
        settleJob?.cancel()
        settleJob = scope.launch {
            delay(HoldScrub.SETTLE_MILLIS)
            endScrub(seek = true, resume = resumesAfterScrub)
        }
    }

    /** Play/pause or select during a pending scrub: seek now and play. */
    fun commitScrub() = endScrub(seek = true, resume = true)

    /** Back during a pending scrub: stay where playback was, in the state it was in. */
    fun cancelScrub() = endScrub(seek = false, resume = resumesAfterScrub)

    fun trackOptions(kind: TrackKind): List<TrackOption> {
        val exo = mutablePlayer.value ?: return emptyList()
        if (!canCommand) return emptyList()
        return PlayerTracks.options(kind, exo.currentTracks, onlineSubtitles, loadedOnline.keys)
    }

    fun select(kind: TrackKind, option: TrackOption) {
        val exo = mutablePlayer.value ?: return
        if (!canCommand) return
        if (kind == TrackKind.Subtitles) startingSubtitle.viewerChose()
        option.online?.let { subtitle -> return loadOnline(exo, subtitle) }
        PlayerTracks.selecting(kind, option.id, exo.currentTracks, exo.trackSelectionParameters)?.let { exo.trackSelectionParameters = it }
    }

    fun mediaInfo(): List<InfoRow> {
        val exo = mutablePlayer.value ?: return emptyList()
        return if (canCommand) PlayerMediaInfo.rows(exo, decoders) else emptyList()
    }

    /** The app left the foreground: pause, save with a remote sync, never resume on return. */
    fun enterBackground() {
        isInBackground = true
        if (scrub != null) endScrub(seek = false, resume = false)
        mutablePlayer.value?.playWhenReady = false
        save(Moment.Background)
    }

    fun enterForeground() {
        isInBackground = false
    }

    private val canCommand: Boolean get() = !isFinished && mutableState.value.hasStarted && !mutableState.value.isFinal

    private fun endScrub(seek: Boolean, resume: Boolean) {
        val ending = scrub ?: return
        settleJob?.cancel()
        settleJob = null
        scrub = null
        mutablePreview.value = null
        val exo = mutablePlayer.value ?: return
        if (seek) {
            exo.seekTo(ending.shownSeconds * MILLIS_PER_SECOND)
            publishClock(exo)
        }
        if (resume) setPaused(false)
    }

    /**
     * Lands on the keyframe beyond [positionMs] in the direction of travel (tvOS mpv `seek relative`),
     * so a skip does not decode a whole group of pictures before the first frame shows. Every other
     * seek stays exact.
     */
    private fun seekToKeyframe(exo: ExoPlayer, positionMs: Long, isForward: Boolean) {
        exo.setSeekParameters(if (isForward) SeekParameters.NEXT_SYNC else SeekParameters.PREVIOUS_SYNC)
        exo.seekTo(positionMs)
        exo.setSeekParameters(SeekParameters.EXACT)
    }

    private fun mediaItem(): MediaItem = MediaItem.Builder()
        .setUri(request.url)
        .setMediaMetadata(PlaybackMetadata.of(request.target))
        .setSubtitleConfigurations(loadedOnline.values.toList())
        .build()

    /** ExoPlayer adds a sideloaded subtitle only with the item, so the item is set again in place. */
    private fun loadOnline(exo: ExoPlayer, subtitle: OnlineSubtitle) {
        loadedOnline[subtitle.id] = MediaItem.SubtitleConfiguration.Builder(Uri.parse(SubtitleUriMarker.mark(subtitle.url)))
            .setId(subtitle.id)
            .setLabel(subtitle.trackTitle)
            .setLanguage(subtitle.language)
            .setMimeType(subtitleMimeType(subtitle.url))
            .build()
        pendingOnlineId = subtitle.id
        pendingAudioFormatId = selectedFormatId(exo, C.TRACK_TYPE_AUDIO)
        exo.setMediaItem(mediaItem(), exo.currentPosition)
        exo.prepare()
    }

    private fun applyPendingSelections(exo: ExoPlayer) {
        val tracks = exo.currentTracks
        pendingOnlineId?.let { id ->
            PlayerTracks.selectingLoaded(id, tracks, exo.trackSelectionParameters)?.let { parameters ->
                exo.trackSelectionParameters = parameters
                pendingOnlineId = null
            }
        }
        pendingAudioFormatId?.let { formatId ->
            val group = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO && (0 until it.length).any { index -> it.getTrackFormat(index).id == formatId } } ?: return
            val index = (0 until group.length).first { group.getTrackFormat(it).id == formatId }
            exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, index))
                .build()
            pendingAudioFormatId = null
        }
    }

    private fun selectedFormatId(exo: ExoPlayer, trackType: Int): String? = exo.currentTracks.groups
        .firstOrNull { it.type == trackType && it.isSelected }
        ?.let { group -> (0 until group.length).firstOrNull(group::isTrackSelected)?.let { group.getTrackFormat(it).id } }

    private val engineListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val exo = mutablePlayer.value ?: return
            if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED, Player.EVENT_IS_PLAYING_CHANGED)) {
                handleEngine(exo)
            }
            if (events.contains(Player.EVENT_TRACKS_CHANGED)) applyPendingSelections(exo)
        }

        override fun onPlayerError(error: PlaybackException) = fail(PlaybackErrorMessages.reason(error))
    }

    private val decoderListener = object : AnalyticsListener {
        override fun onVideoDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {
            decoders = decoders.copy(video = decoderName)
        }

        override fun onAudioDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {
            decoders = decoders.copy(audio = decoderName)
        }
    }

    private fun handleEngine(exo: ExoPlayer) {
        if (isFinished) return
        when (exo.playbackState) {
            Player.STATE_READY -> if (!mutableState.value.hasStarted) startOnce(exo)
            Player.STATE_ENDED -> return handleEnd(exo)
            else -> Unit
        }
        val wasPaused = mutableState.value.isPaused
        update(PlayerEvent.Engine(isBuffering = exo.playbackState == Player.STATE_BUFFERING, playWhenReady = exo.playWhenReady, isPlaying = exo.isPlaying))
        if (mutableState.value.isPaused && !wasPaused) save(Moment.Pause)
        publishClock(exo)
    }

    private fun startOnce(exo: ExoPlayer) {
        if (positionAtLoadMs == null) startPositionMs(durationMs(exo)).takeIf { it > 0 }?.let(exo::seekTo)
        update(PlayerEvent.Started)
        if (!isInBackground) exo.playWhenReady = true
        loadStartingSubtitle(startingSubtitle.tracks(PlayerTracks.embeddedSubtitleLanguages(exo.currentTracks)))
    }

    /** The Settings language's online subtitle, when the file has no track in it (AC ST4). */
    private fun loadStartingSubtitle(subtitle: OnlineSubtitle?) {
        val exo = mutablePlayer.value ?: return
        if (subtitle != null && canCommand) loadOnline(exo, subtitle)
    }

    /** The saved position when it does not depend on the stream's length: the engine opens the stream there. */
    private fun resumePositionAtLoadMs(): Long? {
        if (DebugLaunch.startSecondsBeforeEnd != null || !request.resumes) return null
        return TvosPlayback.resumePositionBeforeLoadMs(request.target)
    }

    /** The start position that needs the stream's length, sought once the engine is ready. */
    private fun startPositionMs(durationMs: Long): Long {
        DebugLaunch.startSecondsBeforeEnd?.let { seconds -> return (durationMs - seconds * MILLIS_PER_SECOND).coerceAtLeast(0) }
        if (!request.resumes || durationMs <= 0) return 0
        return TvosPlayback.resumePositionMs(request.target, durationMs)
    }

    private fun handleEnd(exo: ExoPlayer) {
        update(PlayerEvent.Ended(positionMs = exo.currentPosition, durationMs = durationMs(exo)))
        when (mutableState.value.phase) {
            PlaybackPhase.Ended -> save(Moment.End)
            PlaybackPhase.Failed -> finish()
            else -> Unit
        }
    }

    private fun fail(reason: String) {
        if (isFinished) return
        update(PlayerEvent.Failed(reason))
        finish()
    }

    private fun tick() {
        val exo = mutablePlayer.value ?: return
        publishClock(exo)
        val now = SystemClock.elapsedRealtime()
        val bytes = loadedBytes.get()
        val phase = mutableState.value.phase
        if (phase == PlaybackPhase.Loading) startupDeadline.start(now, bytes) else startupDeadline.cancel()
        if (phase == PlaybackPhase.Buffering) stallWatchdog.watch(now, bytes) else stallWatchdog.stop()
        when {
            startupDeadline.isExpired(now, bytes) -> fail(StartupDeadline.NO_DATA)
            stallWatchdog.hasGivenUp(now, bytes) -> fail(StallWatchdog.STOPPED_SENDING_DATA)
        }
    }

    private fun save(moment: Moment) {
        val exo = mutablePlayer.value ?: return
        if (isFinished) return
        val current = mutableState.value
        val positionMs = exo.currentPosition
        if (!ProgressSavePolicy.shouldSave(moment, current.hasPlayed, positionMs, lastTickPositionMs)) return
        if (moment == Moment.Tick) lastTickPositionMs = positionMs
        val isEnded = current.phase == PlaybackPhase.Ended
        val durationMs = durationMs(exo)
        session.save(
            positionMs = ProgressSavePolicy.savedPositionMs(isEnded, positionMs, durationMs),
            durationMs = durationMs,
            isPlaying = current.phase == PlaybackPhase.Playing,
            isEnded = isEnded,
            syncRemote = moment.syncsRemote
        )
    }

    private fun update(event: PlayerEvent) = mutableState.update { it.on(event) }

    private fun publishClock(exo: ExoPlayer) {
        mutableClock.value = PlaybackClock(
            positionSeconds = (exo.currentPosition / MILLIS_PER_SECOND).toInt(),
            durationSeconds = (durationMs(exo) / MILLIS_PER_SECOND).toInt(),
            bufferedSeconds = (exo.bufferedPosition / MILLIS_PER_SECOND).toInt()
        )
    }

    private suspend fun tickEvery(intervalMs: Long, action: () -> Unit) {
        while (scope.isActive) {
            delay(intervalMs)
            action()
        }
    }

    private fun durationMs(exo: ExoPlayer): Long = exo.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0

    private fun subtitleMimeType(url: String): String {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            path.endsWith(".vtt") -> MimeTypes.TEXT_VTT

            path.endsWith(".ass") || path.endsWith(".ssa") -> MimeTypes.TEXT_SSA

            path.endsWith(".ttml") || path.endsWith(".dfxp") -> MimeTypes.APPLICATION_TTML

            // OpenSubtitles links often have no extension; they serve SubRip.
            else -> MimeTypes.APPLICATION_SUBRIP
        }
    }

    /** Counts the network bytes the engine loads, for [StartupDeadline] and [StallWatchdog]. */
    private class ByteCounter(private val bytes: AtomicLong) : TransferListener {
        override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit

        override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit

        override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
            bytes.addAndGet(bytesTransferred.toLong())
        }

        override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
    }

    private companion object {
        const val CLOCK_TICK_MS = 500L
        const val MILLIS_PER_SECOND = 1_000L
    }
}
