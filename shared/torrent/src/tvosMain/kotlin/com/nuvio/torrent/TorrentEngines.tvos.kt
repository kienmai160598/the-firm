package com.nuvio.torrent

import cnames.structs.movies_engine
import com.nuvio.torrent.native.MOVIES_ENGINE_INVALID_ARGUMENT
import com.nuvio.torrent.native.MOVIES_ENGINE_NOT_FOUND
import com.nuvio.torrent.native.MOVIES_ENGINE_OK
import com.nuvio.torrent.native.movies_engine_config
import com.nuvio.torrent.native.movies_engine_create
import com.nuvio.torrent.native.movies_engine_last_error
import com.nuvio.torrent.native.movies_engine_version
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

@OptIn(ExperimentalForeignApi::class)
actual object TorrentEngines {
    actual val version: String
        get() = movies_engine_version()?.toKString().orEmpty()

    actual fun open(config: TorrentEngineConfig): TorrentEngine = memScoped {
        val nativeConfig = alloc<movies_engine_config>()
        nativeConfig.state_directory = config.stateDirectory.cstr.ptr
        val handle = alloc<CPointerVar<movies_engine>>()
        throwIfFailed(movies_engine_create(nativeConfig.ptr, handle.ptr))
        NativeTorrentEngine(checkNotNull(handle.value) { "engine created without a handle" })
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun throwIfFailed(status: Int) {
    if (status == MOVIES_ENGINE_OK) return
    val reason = when (status) {
        MOVIES_ENGINE_INVALID_ARGUMENT -> TorrentEngineException.Reason.InvalidArgument
        MOVIES_ENGINE_NOT_FOUND -> TorrentEngineException.Reason.NotFound
        else -> TorrentEngineException.Reason.Internal
    }
    throw TorrentEngineException(reason, movies_engine_last_error()?.toKString().orEmpty())
}
