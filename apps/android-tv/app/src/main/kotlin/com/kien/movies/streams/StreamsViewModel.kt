package com.kien.movies.streams

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kien.movies.bridge.BridgeFlows
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.player.PlaybackRequest
import com.kien.movies.player.StreamNotice
import com.kien.movies.torrent.TorrentSession
import com.kien.movies.torrent.TorrentStartResult
import com.nuvio.tvos.TvosLastLinkAttempt
import com.nuvio.tvos.TvosResumePoint
import com.nuvio.tvos.TvosStream
import com.nuvio.tvos.TvosStreamGroup
import com.nuvio.tvos.TvosStreamTarget
import com.nuvio.tvos.TvosStreams
import com.nuvio.tvos.TvosStreamsEmptyState
import com.nuvio.tvos.TvosStreamsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the body of Streams shows (tvOS StreamsPhase plus its data). */
sealed interface StreamsContent {
    data object Loading : StreamsContent

    data class Empty(val emptyState: TvosStreamsEmptyState) : StreamsContent

    /** [searchingAddonCount] addons have not answered yet ("Searching N more addons", S1). */
    data class Groups(val groups: List<StreamGroupItem>, val searchingAddonCount: Int) : StreamsContent {
        val hasRows: Boolean get() = groups.any { group -> group.rows.isNotEmpty() }

        /** Every row of every addon: the header's "N sources" (tvOS StreamsModel.sourceCount). */
        val sourceCount: Int get() = groups.sumOf { group -> group.rows.size }
    }
}

/** Everything the Streams screen draws, as one state. */
@Immutable
data class StreamsUiState(
    val content: StreamsContent = StreamsContent.Loading,
    val selection: StreamSelection? = null,
    val directResume: TvosResumePoint? = null,
    val sheet: StreamSheet? = null,
    val pendingResume: PendingResume? = null,
    val playback: PlaybackRequest? = null,
    /**
     * A Continue Watching card's last link starts or plays: the opening cover hides the list on
     * the way in and out, until the link fails (AC H4).
     */
    val isOpeningLastLink: Boolean = false
)

/**
 * The streams of one movie or episode and the path from a press to the player (tvOS
 * StreamsModel.swift plus StreamsPlayback.swift). ExoPlayer plays everything in v1, so there is
 * no engine preflight: "Checking stream" lasts while the resume point loads. A torrent first
 * starts in the engine, its progress on the row, and then plays from its local URL (AC T1).
 *
 * @param resumesDirectly opened from Continue Watching: the header shows the resume bar, a
 *   press resumes without asking, and the link the title last played from plays at once (AC H4).
 */
class StreamsViewModel(val target: TvosStreamTarget, private val resumesDirectly: Boolean) : ViewModel() {
    private val interaction = MutableStateFlow(StreamsUiState())
    private val torrent = TorrentSession(viewModelScope)
    private val lastLink = if (resumesDirectly) TvosLastLinkAttempt(target) else null

    private val content = BridgeFlows.streams(target)
        .map { streams -> streamsContent(streams, debugFixture()) }
        .flowOn(Dispatchers.Default)
        .onEach(::openDebugStream)

    val state: StateFlow<StreamsUiState> = combine(content, interaction) { content, interaction ->
        interaction.copy(content = content)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StreamsUiState())

    init {
        loadDirectResume()
        lastLink?.take()?.let { stream ->
            interaction.update { state -> state.copy(isOpeningLastLink = true) }
            select(stream)
        }
    }

    fun reload() {
        TvosStreams.reload(target)
    }

    fun select(stream: TvosStream) {
        val current = interaction.value
        val isBusy = current.selection != null || current.playback != null || current.pendingResume != null
        when (val press = streamPress(stream, isBusy)) {
            StreamPress.Ignored -> Unit

            is StreamPress.OpensSheet -> interaction.update { state -> state.copy(sheet = press.sheet, isOpeningLastLink = false) }

            is StreamPress.Checks -> {
                interaction.update { state -> state.copy(selection = StreamSelection(stream.key, StreamRowProgress.checking)) }
                chooseStart(press.stream)
            }

            is StreamPress.StartsTorrent -> startTorrent(press.stream)
        }
    }

    /** The resume dialog's answer; null closes it without playing. */
    fun answerResume(resumes: Boolean?) {
        val pending = interaction.value.pendingResume ?: return
        if (resumes == null) {
            interaction.update { state -> state.copy(pendingResume = null) }
        } else {
            start(pending.stream, resumes)
        }
    }

    fun closeSheet() {
        interaction.update { state -> state.copy(sheet = null) }
    }

    /** The player closed the cover because the stream failed (P4): its notice opens the sheet. */
    fun playbackFailed(notice: StreamNotice) {
        interaction.update { state ->
            val stream = state.playback?.stream ?: return@update state
            state.copy(playback = null, selection = null, isOpeningLastLink = false, sheet = StreamSheet(stream, notice(stream, notice)))
        }
        torrent.stop()
        loadDirectResume()
    }

    /**
     * The user left the player. Returns true when Streams closes with it: it played the last link
     * (AC H4), whose opening cover is still up, so the list never shows.
     */
    fun playbackEnded(): Boolean {
        interaction.update { state -> state.copy(playback = null, selection = null) }
        torrent.stop()
        if (interaction.value.isOpeningLastLink) return true
        loadDirectResume()
        return false
    }

    override fun onCleared() {
        torrent.stop()
    }

    private fun startTorrent(stream: TvosStream) {
        interaction.update { state -> state.copy(selection = StreamSelection(stream.key, StreamRowProgress.startingTorrent)) }
        val progressJob = viewModelScope.launch {
            torrent.status.collect { status -> showProgress(stream.key, torrentProgress(status)) }
        }
        torrent.start(stream) { result ->
            progressJob.cancel()
            when (result) {
                is TorrentStartResult.Started -> {
                    showProgress(stream.key, StreamRowProgress.checking)
                    chooseStart(stream)
                }

                is TorrentStartResult.Failed -> interaction.update { state ->
                    state.copy(
                        selection = null,
                        isOpeningLastLink = false,
                        sheet = StreamSheet(stream, notice(stream, StreamNotice(TORRENT_FAILED_TITLE, result.message)))
                    )
                }
            }
        }
    }

    /** Shows [progress] on the pressed row, if it is still [streamKey]'s. */
    private fun showProgress(streamKey: String, progress: StreamRowProgress) {
        interaction.update { state ->
            if (state.selection?.streamKey == streamKey) state.copy(selection = StreamSelection(streamKey, progress)) else state
        }
    }

    private fun chooseStart(stream: TvosStream) {
        if (resumesDirectly) {
            start(stream, resumes = true)
            return
        }
        viewModelScope.launch {
            val point = BridgeFlows.resumePoint(target)
            if (point == null) start(stream, resumes = false) else offerResume(PendingResume(stream, point))
        }
    }

    private fun offerResume(pending: PendingResume) {
        DebugLaunch.resumeChoice?.let { resumes ->
            start(pending.stream, resumes)
            return
        }
        interaction.update { state -> state.copy(selection = null, isOpeningLastLink = false, pendingResume = pending) }
    }

    private fun start(stream: TvosStream, resumes: Boolean) {
        val request = torrent.localUrl(stream)?.let { url -> PlaybackRequest(target, stream, url, resumes) }
            ?: PlaybackRequest.of(target, stream, resumes)
        interaction.update { state ->
            state.copy(
                pendingResume = null,
                selection = null,
                playback = request,
                sheet = if (request == null) StreamSheet(stream, notice(stream, null)) else state.sheet,
                isOpeningLastLink = state.isOpeningLastLink && request != null
            )
        }
    }

    /** [notice], or the last link's notice when [stream] was the last link (now forgotten). */
    private fun notice(stream: TvosStream, notice: StreamNotice?): StreamNotice? = if (lastLink?.failed(stream.key) == true) StreamNotice.lastLinkFailed(notice) else notice

    private fun loadDirectResume() {
        if (!resumesDirectly) return
        viewModelScope.launch {
            val point = BridgeFlows.resumePoint(target)
            interaction.update { state -> state.copy(directResume = point) }
        }
    }

    private fun openDebugStream(content: StreamsContent) {
        if (!DebugLaunch.opensFirstStream) return
        val current = interaction.value
        if (current.sheet != null || current.playback != null) return
        val first = (content as? StreamsContent.Groups)?.groups?.firstNotNullOfOrNull { group -> group.rows.firstOrNull() } ?: return
        DebugLaunch.markFirstStreamOpened()
        select(first.stream)
    }

    private fun debugFixture(): TvosStreamGroup? = if (DebugLaunch.usesStreamFixture) StreamsDebugFixture.group else null

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val TORRENT_FAILED_TITLE = "The torrent did not start"
    }
}

/**
 * The body of Streams for [streams] (tvOS StreamsModel.phase, visibleGroups,
 * searchingAddonCount). A group shows once it has streams or an error; the empty state wins.
 * [fixture] (debug launch only) is put first once loading ends and hides the empty state.
 */
fun streamsContent(streams: TvosStreamsState, fixture: TvosStreamGroup? = null): StreamsContent {
    val useFixture = fixture != null && !streams.isLoading
    val groups = if (useFixture) listOf(fixture) + streams.groups.filter { group -> group.streams.isNotEmpty() } else streams.groups
    val emptyState = streams.emptyState.takeUnless { useFixture }
    val visible = groups.filter { group -> group.streams.isNotEmpty() || group.errorMessage != null }
    return when {
        emptyState != null -> StreamsContent.Empty(emptyState)

        visible.isEmpty() -> StreamsContent.Loading

        else -> StreamsContent.Groups(
            groups = visible.map(::streamGroupItem),
            searchingAddonCount = groups.count { group -> group.isLoading && group.streams.isEmpty() }
        )
    }
}

private fun streamGroupItem(group: TvosStreamGroup) = StreamGroupItem(
    addonId = group.addonId,
    addonName = group.addonName,
    errorMessage = group.errorMessage.takeIf { group.streams.isEmpty() },
    rows = group.streams.map(::streamRowItem)
)
