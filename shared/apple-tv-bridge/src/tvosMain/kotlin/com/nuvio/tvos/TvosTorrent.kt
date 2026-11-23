package com.nuvio.tvos

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Torrent streams for the Swift app. Which engine runs them is picked at build time
 * (`nuvio.tvos.engine`, docs/engine-rebuild.md D2): TvosTorrentBackend comes from
 * src/tvosLegacyEngine or src/tvosNextEngine.
 */
object TvosTorrent {
    private val mainScope = MainScope()

    fun start(stream: TvosStream, onResult: (TvosTorrentStart) -> Unit): TvosCancellable {
        val job = mainScope.launch { onResult(TvosTorrentBackend.start(stream)) }
        return TvosCancellable(job)
    }

    fun observe(onChange: (TvosTorrentStatus) -> Unit): TvosCancellable {
        val job = mainScope.launch {
            TvosTorrentBackend.statuses().distinctUntilChanged().collect(onChange)
        }
        return TvosCancellable(job)
    }

    fun stop() {
        TvosTorrentBackend.stop()
    }
}
