package com.nuvio.torrent

/** The native torrent engine. The app opens one and closes it when it no longer streams. */
interface TorrentEngine : AutoCloseable

data class TorrentEngineConfig(
    /** Absolute path of a directory the engine may own. */
    val stateDirectory: String
)

class TorrentEngineException(
    val reason: Reason,
    message: String
) : Exception(message) {
    enum class Reason { InvalidArgument, Internal }
}

/** Opens engines. Throws [TorrentEngineException] when the native call fails. */
expect object TorrentEngines {
    val version: String

    fun open(config: TorrentEngineConfig): TorrentEngine
}
