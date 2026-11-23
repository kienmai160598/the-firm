package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.p2p.P2pStreamingEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val NO_INFO_HASH = "This stream has no torrent info hash"
private const val START_FAILED = "Unable to start torrent stream"

object TvosTorrent {
    private val log = Logger.withTag("TvosTorrent")
    private val mainScope = MainScope()

    fun start(stream: TvosStream, onResult: (TvosTorrentStart) -> Unit): TvosCancellable {
        val job = mainScope.launch { onResult(startOrFailure(stream)) }
        return TvosCancellable(job)
    }

    fun observe(onChange: (TvosTorrentStatus) -> Unit): TvosCancellable {
        val job = mainScope.launch {
            P2pStreamingEngine.state
                .map(::tvosTorrentStatus)
                .distinctUntilChanged()
                .collect(onChange)
        }
        return TvosCancellable(job)
    }

    fun stop() {
        P2pStreamingEngine.stopStream()
    }

    private suspend fun startOrFailure(stream: TvosStream): TvosTorrentStart {
        val request = stream.torrentRequest()
            ?: return TvosTorrentStart(localUrl = null, errorMessage = NO_INFO_HASH)

        return try {
            val localUrl = P2pStreamingEngine.startStream(request)
            log.i { "Torrent ${request.infoHash} file ${request.fileIdx} at $localUrl" }
            TvosTorrentStart(localUrl = localUrl, errorMessage = null)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            log.w { "Torrent ${request.infoHash} failed: ${error.message}" }
            TvosTorrentStart(localUrl = null, errorMessage = error.message ?: START_FAILED)
        }
    }
}
