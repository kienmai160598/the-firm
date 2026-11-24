package com.nuvio.torrent

// Status and stream state values of engine/include/movies_engine/movies_engine.h.
private const val MOVIES_ENGINE_OK = 0
private const val MOVIES_ENGINE_INVALID_ARGUMENT = 1
private const val MOVIES_ENGINE_NOT_FOUND = 3
internal const val MOVIES_ENGINE_STREAM_READY = 1L
internal const val MOVIES_ENGINE_STREAM_FAILED = 2L

actual object TorrentEngines {
    actual val version: String
        get() = MoviesEngineJni.version()?.decodeToString().orEmpty()

    actual fun open(config: TorrentEngineConfig): TorrentEngine {
        val handle = LongArray(1)
        val error = nativeError()
        val status = MoviesEngineJni.create(
            config.stateDirectory.encodeToByteArray(),
            config.budget.toNative(),
            handle,
            error
        )
        throwIfFailed(status, error)
        check(handle[0] != 0L) { "engine created without a handle" }
        return JniTorrentEngine(handle[0])
    }
}

/** No budget is four zeros: the engine keeps its defaults. */
private fun TorrentEngineBudget?.toNative(): LongArray = if (this == null) {
    LongArray(4)
} else {
    longArrayOf(storeCapBytes, aheadBytes, behindBytes, evictionThresholdBytes)
}

/** The slot a failed native call writes its message to. */
internal fun nativeError(): Array<ByteArray?> = arrayOfNulls(1)

internal fun throwIfFailed(status: Int, error: Array<ByteArray?>) {
    if (status == MOVIES_ENGINE_OK) return
    val reason = when (status) {
        MOVIES_ENGINE_INVALID_ARGUMENT -> TorrentEngineException.Reason.InvalidArgument
        MOVIES_ENGINE_NOT_FOUND -> TorrentEngineException.Reason.NotFound
        else -> TorrentEngineException.Reason.Internal
    }
    throw TorrentEngineException(reason, error[0]?.decodeToString().orEmpty())
}
