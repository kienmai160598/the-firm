package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.p2p.buildP2pMagnetUri
import com.nuvio.torrent.ANY_FILE
import com.nuvio.torrent.TorrentEngine
import com.nuvio.torrent.awaitReady
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Both apps' torrent streams on the new engine (engine/, docs/engine-rebuild.md): payload in
 * memory, served on 127.0.0.1. Each platform's TvosTorrentBackend gives it [openEngine], which
 * opens the engine once with that platform's state directory and budget. Called from the main
 * thread, like the rest of the bridge. The engine reports phases only: no peers, speed or buffer
 * until E4, so the status carries the step and zeros.
 */
internal class EngineTorrentBackend(
    private val log: Logger,
    private val openEngine: suspend () -> TorrentEngine
) {
    private val status = MutableStateFlow(quietStatus(TORRENT_PHASE_IDLE, errorMessage = null))
    private var engine: TorrentEngine? = null
    private var currentStream: Long? = null

    suspend fun start(stream: TvosStream): TvosTorrentStart {
        stop()
        val request = stream.torrentRequest()
            ?: return TvosTorrentStart(localUrl = null, errorMessage = NO_INFO_HASH)
        status.value = connectingStatus()
        val started = TimeSource.Monotonic.markNow()
        return try {
            val activeEngine = engine ?: openEngine().also { opened -> engine = opened }
            val magnet = buildP2pMagnetUri(request.infoHash, (request.trackers + publicTrackers).distinct())
            val id = activeEngine.openStream(magnet, request.fileIdx ?: ANY_FILE)
            currentStream = id
            val ready = activeEngine.awaitReady(id)
            log.i { "Torrent ${request.infoHash} file ${ready.fileIndex} ready in ${started.elapsedNow()} at ${ready.url}" }
            status.value = streamingStatus(ready.url)
            TvosTorrentStart(localUrl = ready.url, errorMessage = null)
        } catch (cancellation: CancellationException) {
            stop()
            throw cancellation
        } catch (error: Exception) {
            log.w { "Torrent ${request.infoHash} failed after ${started.elapsedNow()}: ${error.message}" }
            stop()
            val message = error.message ?: START_FAILED
            status.value = quietStatus(TORRENT_PHASE_FAILED, message)
            TvosTorrentStart(localUrl = null, errorMessage = message)
        }
    }

    fun statuses(): Flow<TvosTorrentStatus> = status

    fun stop() {
        val stream = currentStream ?: return
        currentStream = null
        runCatching { engine?.closeStream(stream) }
            .onFailure { error -> log.w { "Closing stream $stream failed: ${error.message}" } }
        status.value = quietStatus(TORRENT_PHASE_IDLE, errorMessage = null)
    }

    private fun connectingStatus(): TvosTorrentStatus = quietStatus(TORRENT_PHASE_IDLE, errorMessage = null)
        .copy(phase = TORRENT_PHASE_CONNECTING, step = TORRENT_STEP_RESOLVING)

    private fun streamingStatus(url: String): TvosTorrentStatus = quietStatus(TORRENT_PHASE_IDLE, errorMessage = null)
        .copy(phase = TORRENT_PHASE_STREAMING, localUrl = url)

    private companion object {
        /** Well known open trackers, added to the ones the add-on sends. */
        val publicTrackers = listOf(
            "udp://tracker.opentrackr.org:1337/announce",
            "udp://open.stealth.si:80/announce",
            "udp://tracker.torrent.eu.org:451/announce",
            "udp://exodus.desync.com:6969/announce",
            "udp://open.demonii.com:1337/announce",
        )
    }
}
