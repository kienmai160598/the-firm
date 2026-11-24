package com.nuvio.tvos

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Torrent streams for both TV apps. The engine behind them is per platform: see
 * TvosTorrentBackend.kt.
 */
object TvosTorrent {
    private val mainScope = MainScope()

    fun start(stream: TvosStream, onResult: (TvosTorrentStart) -> Unit): TvosCancellable {
        val job = mainScope.launch { onResult(startTorrent(stream)) }
        return TvosCancellable(job)
    }

    fun observe(onChange: (TvosTorrentStatus) -> Unit): TvosCancellable {
        val job = mainScope.launch {
            torrentStatuses().distinctUntilChanged().collect(onChange)
        }
        return TvosCancellable(job)
    }

    fun stop() {
        stopTorrent()
    }
}
