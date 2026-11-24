package com.nuvio.tvos

import android.app.ActivityManager
import android.content.Context
import co.touchlab.kermit.Logger
import com.nuvio.torrent.TorrentEngine
import com.nuvio.torrent.TorrentEngineConfig
import com.nuvio.torrent.TorrentEngines
import com.nuvio.torrent.budgetFor
import java.io.File
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Android opens the new engine (EngineTorrentBackend) in the app's cache directory, payload in
 * RAM only, with a budget sized from the memory free when it opens (budgetFor in shared/torrent).
 * TvRuntime.init calls [initialize] before any torrent starts.
 */
internal object TvosTorrentBackend {
    private val log = Logger.withTag("TvosTorrentNext")
    private var appContext: Context? = null

    val engine = EngineTorrentBackend(log) { withContext(Dispatchers.IO) { openEngine() } }

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun openEngine(): TorrentEngine {
        val context = checkNotNull(appContext) { "TvRuntime.init has not run" }
        val memory = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(memory)
        val budget = budgetFor(memory.availMem)
        val directory = File(context.cacheDir, "movies-engine").absolutePath
        val started = TimeSource.Monotonic.markNow()
        return TorrentEngines.open(TorrentEngineConfig(directory, budget)).also {
            log.i { "Engine ${TorrentEngines.version} open in ${started.elapsedNow()}, ${memory.availMem shr 20} MiB free, $budget" }
        }
    }
}

internal actual suspend fun startTorrent(stream: TvosStream): TvosTorrentStart = TvosTorrentBackend.engine.start(stream)

internal actual fun torrentStatuses(): Flow<TvosTorrentStatus> = TvosTorrentBackend.engine.statuses()

internal actual fun stopTorrent() = TvosTorrentBackend.engine.stop()
