package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.p2p.buildP2pMagnetUri
import com.nuvio.torrent.ANY_FILE
import com.nuvio.torrent.TorrentEngine
import com.nuvio.torrent.TorrentEngineConfig
import com.nuvio.torrent.TorrentEngines
import com.nuvio.torrent.awaitReady
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * The new engine (engine/, docs/engine-rebuild.md): payload in memory, served on 127.0.0.1. Runs
 * on the main thread like the rest of the bridge. E1 reports phases only; peers, speed and buffer
 * come with E4.
 */
internal object TvosTorrentBackend {
    private val log = Logger.withTag("TvosTorrentNext")
    private val status = MutableStateFlow(quietStatus(TORRENT_PHASE_IDLE, errorMessage = null))
    private var engine: TorrentEngine? = null
    private var currentStream: Long? = null

    suspend fun start(stream: TvosStream): TvosTorrentStart {
        stop()
        val request = stream.torrentRequest()
            ?: return TvosTorrentStart(localUrl = null, errorMessage = NO_INFO_HASH)
        status.value = connectingStatus()
        return try {
            val activeEngine = openEngine()
            val magnet = buildP2pMagnetUri(request.infoHash, (request.trackers + publicTrackers).distinct())
            val id = activeEngine.openStream(magnet, request.fileIdx ?: ANY_FILE)
            currentStream = id
            val ready = activeEngine.awaitReady(id)
            log.i { "Torrent ${request.infoHash} file ${ready.fileIndex} at ${ready.url}" }
            status.value = streamingStatus(ready.url)
            TvosTorrentStart(localUrl = ready.url, errorMessage = null)
        } catch (cancellation: CancellationException) {
            stop()
            throw cancellation
        } catch (error: Exception) {
            log.w { "Torrent ${request.infoHash} failed: ${error.message}" }
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

    private fun openEngine(): TorrentEngine =
        engine ?: TorrentEngines.open(TorrentEngineConfig(stateDirectory())).also { engine = it }

    private fun stateDirectory(): String {
        val caches = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String
        return checkNotNull(caches) { "No caches directory" } + "/movies-engine"
    }

    private fun connectingStatus(): TvosTorrentStatus =
        quietStatus(TORRENT_PHASE_IDLE, errorMessage = null)
            .copy(phase = TORRENT_PHASE_CONNECTING, step = "resolving")

    private fun streamingStatus(url: String): TvosTorrentStatus =
        quietStatus(TORRENT_PHASE_IDLE, errorMessage = null)
            .copy(phase = TORRENT_PHASE_STREAMING, localUrl = url)

    /** Well known open trackers, added to the ones the add-on sends. */
    private val publicTrackers = listOf(
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.stealth.si:80/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://exodus.desync.com:6969/announce",
        "udp://open.demonii.com:1337/announce",
    )
}

internal actual suspend fun startTorrent(stream: TvosStream): TvosTorrentStart = TvosTorrentBackend.start(stream)

internal actual fun torrentStatuses(): Flow<TvosTorrentStatus> = TvosTorrentBackend.statuses()

internal actual fun stopTorrent() = TvosTorrentBackend.stop()
