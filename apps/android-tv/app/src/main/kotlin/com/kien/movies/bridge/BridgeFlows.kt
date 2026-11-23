package com.kien.movies.bridge

import com.nuvio.tvos.TvosAddon
import com.nuvio.tvos.TvosAddonInstallResult
import com.nuvio.tvos.TvosAddons
import com.nuvio.tvos.TvosCancellable
import com.nuvio.tvos.TvosDetails
import com.nuvio.tvos.TvosDetailsState
import com.nuvio.tvos.TvosHome
import com.nuvio.tvos.TvosHomeState
import com.nuvio.tvos.TvosPlayback
import com.nuvio.tvos.TvosResumePoint
import com.nuvio.tvos.TvosSearch
import com.nuvio.tvos.TvosSearchState
import com.nuvio.tvos.TvosStream
import com.nuvio.tvos.TvosStreamTarget
import com.nuvio.tvos.TvosStreams
import com.nuvio.tvos.TvosStreamsState
import com.nuvio.tvos.TvosSubtitle
import com.nuvio.tvos.TvosSubtitles
import com.nuvio.tvos.TvosTorrent
import com.nuvio.tvos.TvosTorrentStart
import com.nuvio.tvos.TvosTorrentStatus
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Every callback entry point of the `Tvos*` API that a screen needs, as a cold [Flow] (observe
 * calls) or a suspend function (one shot results). Collection end or coroutine cancellation
 * cancels the bridge job. The bridge calls back on the main thread.
 *
 * State flows are conflated: a screen only needs the latest state, and a full channel would
 * otherwise drop updates silently.
 *
 * The plain synchronous calls are made on the `Tvos*` objects directly, they need no wrapper:
 * `TvosHome.removeFromContinueWatching`, `TvosStreams.reload`, `TvosSearch.search`, `clear`,
 * `recordRecentSearch`, `removeRecentSearch`, `TvosAddons.current`, `remove`, `setEnabled`,
 * `move`, `TvosPlayback.resumePositionMs`, `startSession` (then `TvosPlaybackSession.save`),
 * `TvosTorrent.stop`.
 */
object BridgeFlows {
    fun home(): Flow<TvosHomeState> = observe { onChange -> TvosHome.observeHome(onChange) }

    fun details(type: String, id: String): Flow<TvosDetailsState> = observe { onChange -> TvosDetails.observeDetails(type, id, onChange) }

    fun streams(target: TvosStreamTarget): Flow<TvosStreamsState> = observe { onChange -> TvosStreams.observeStreams(target, onChange) }

    fun search(): Flow<TvosSearchState> = observe { onChange -> TvosSearch.observe(onChange) }

    fun addons(): Flow<List<TvosAddon>> = observe { onChange -> TvosAddons.observe(onChange) }

    fun torrentStatus(): Flow<TvosTorrentStatus> = observe { onChange -> TvosTorrent.observe(onChange) }

    /** Installs the addon at [rawUrl] (trimmed by the bridge) and returns why it failed, if it did. */
    suspend fun installAddon(rawUrl: String): TvosAddonInstallResult = suspendCancellableCoroutine { continuation ->
        TvosAddons.install(rawUrl) { result -> continuation.resume(result) }
    }

    /** The saved resume point of [target], or null when there is none. */
    suspend fun resumePoint(target: TvosStreamTarget): TvosResumePoint? = suspendCancellableCoroutine { continuation ->
        TvosPlayback.resumePoint(target) { point -> continuation.resume(point) }
    }

    /** Online subtitles for [stream] from the installed subtitle addons, or OpenSubtitles. */
    suspend fun subtitles(target: TvosStreamTarget, stream: TvosStream): List<TvosSubtitle> = oneShot { onResult -> TvosSubtitles.load(target, stream, onResult) }

    /** Starts the torrent engine for [stream]; cancelling the caller cancels the start. */
    suspend fun startTorrent(stream: TvosStream): TvosTorrentStart = oneShot { onResult -> TvosTorrent.start(stream, onResult) }

    private fun <T> observe(start: (onChange: (T) -> Unit) -> TvosCancellable): Flow<T> = callbackFlow {
        val observation = start { value -> trySend(value) }
        awaitClose { observation.cancel() }
    }.conflate()

    private suspend fun <T> oneShot(start: (onResult: (T) -> Unit) -> TvosCancellable): T = suspendCancellableCoroutine { continuation ->
        val job = start { value -> continuation.resume(value) }
        continuation.invokeOnCancellation { job.cancel() }
    }
}
