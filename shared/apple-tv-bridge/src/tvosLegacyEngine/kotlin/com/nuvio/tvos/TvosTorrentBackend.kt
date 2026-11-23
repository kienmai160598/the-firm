package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.p2p.P2pStreamingEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The old engine (nuvio-engine through upstream's P2pStreamingEngine), until slice E7. */
internal object TvosTorrentBackend {
    private val log = Logger.withTag("TvosTorrent")

    suspend fun start(stream: TvosStream): TvosTorrentStart {
        TvosTorrentDefaults.apply()
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

    fun statuses(): Flow<TvosTorrentStatus> = P2pStreamingEngine.state.map(::tvosTorrentStatus)

    fun stop() {
        P2pStreamingEngine.stopStream()
    }
}
