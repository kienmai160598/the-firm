package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.torrent.TorrentEngineConfig
import com.nuvio.torrent.TorrentEngines
import kotlinx.coroutines.flow.Flow
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * tvOS opens the new engine (EngineTorrentBackend) in its caches directory with no budget, so it
 * keeps the engine's default: 512 MiB cap, 256 MiB ahead, until E3 sizes it from memory.
 */
internal object TvosTorrentBackend {
    val engine = EngineTorrentBackend(Logger.withTag("TvosTorrentNext")) {
        TorrentEngines.open(TorrentEngineConfig(stateDirectory()))
    }

    private fun stateDirectory(): String {
        val caches = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String
        return checkNotNull(caches) { "No caches directory" } + "/movies-engine"
    }
}

internal actual suspend fun startTorrent(stream: TvosStream): TvosTorrentStart = TvosTorrentBackend.engine.start(stream)

internal actual fun torrentStatuses(): Flow<TvosTorrentStatus> = TvosTorrentBackend.engine.statuses()

internal actual fun stopTorrent() = TvosTorrentBackend.engine.stop()
