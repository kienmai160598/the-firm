package com.kien.movies.torrent

import com.kien.movies.bridge.BridgeFlows
import com.nuvio.tvos.TvosStream
import com.nuvio.tvos.TvosTorrent
import com.nuvio.tvos.TvosTorrentStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * One torrent start for the Streams screen (tvOS TorrentSession.swift): the engine's status while
 * it starts, the local URL once it can play, and the engine stop on exit (AC T1). Not a screen;
 * StreamsViewModel owns it with its viewModelScope.
 */
class TorrentSession(private val scope: CoroutineScope) {
    private val mutableStatus = MutableStateFlow<TvosTorrentStatus?>(null)

    /** The engine's latest status since the last [start], null before its first tick. */
    val status: StateFlow<TvosTorrentStatus?> = mutableStatus.asStateFlow()

    private var startJob: Job? = null
    private var statusJob: Job? = null
    private val localUrls = mutableMapOf<String, String>()

    /** The local URL of [stream] once its torrent started, else null. */
    fun localUrl(stream: TvosStream): String? = localUrls[stream.key]

    /** Starts the torrent of [stream], replacing a start still running; [onResult] runs once. */
    fun start(stream: TvosStream, onResult: (TorrentStartResult) -> Unit) {
        startJob?.cancel()
        observeStatus()
        startJob = scope.launch {
            val start = BridgeFlows.startTorrent(stream)
            val url = start.localUrl
            val result = if (url == null) {
                TorrentStartResult.Failed(start.errorMessage ?: START_FAILED)
            } else {
                localUrls[stream.key] = url
                TorrentStartResult.Started(url)
            }
            startJob = null
            onResult(result)
        }
    }

    /** Cancels a start, forgets the local URLs and stops the engine. */
    fun stop() {
        startJob?.cancel()
        startJob = null
        statusJob?.cancel()
        statusJob = null
        mutableStatus.value = null
        localUrls.clear()
        TvosTorrent.stop()
    }

    private fun observeStatus() {
        if (statusJob != null) return
        statusJob = scope.launch {
            BridgeFlows.torrentStatus().collect { status -> mutableStatus.value = status }
        }
    }

    private companion object {
        const val START_FAILED = "The torrent could not be started."
    }
}

/** How a torrent start ended. */
sealed interface TorrentStartResult {
    data class Started(val localUrl: String) : TorrentStartResult

    data class Failed(val message: String) : TorrentStartResult
}
