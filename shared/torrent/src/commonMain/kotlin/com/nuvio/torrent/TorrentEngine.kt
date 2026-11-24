package com.nuvio.torrent

/** Plays the largest video file of a torrent. */
const val ANY_FILE = -1

/**
 * The native torrent engine. The app opens one and closes it when it no longer streams. A stream
 * keeps its payload in memory and is served to the player over HTTP on 127.0.0.1.
 */
interface TorrentEngine : AutoCloseable {
    /** Starts a stream and returns its id at once; poll [streamInfo] until it is ready. */
    fun openStream(magnetUri: String, fileIndex: Int = ANY_FILE): Long

    /** Throws [TorrentEngineException] with [TorrentEngineException.Reason.NotFound] once closed. */
    fun streamInfo(stream: Long): StreamInfo

    fun closeStream(stream: Long)
}

sealed interface StreamInfo {
    /** Waiting for the torrent's metadata. */
    data object Resolving : StreamInfo

    data class Ready(
        val url: String,
        val fileIndex: Int,
        val fileSize: Long,
        val fileName: String
    ) : StreamInfo

    data class Failed(val message: String) : StreamInfo
}

data class TorrentEngineConfig(
    /** Absolute path of a directory the engine may own. */
    val stateDirectory: String
)

class TorrentEngineException(
    val reason: Reason,
    message: String
) : Exception(message) {
    enum class Reason { InvalidArgument, Internal, NotFound }
}

/** Opens engines. Throws [TorrentEngineException] when the native call fails. */
expect object TorrentEngines {
    val version: String

    fun open(config: TorrentEngineConfig): TorrentEngine
}
