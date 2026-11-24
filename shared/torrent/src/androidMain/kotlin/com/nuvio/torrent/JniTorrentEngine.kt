package com.nuvio.torrent

/** Owns one native engine handle; [close] releases it once (tvOS: NativeTorrentEngine). */
internal class JniTorrentEngine(private var handle: Long) : TorrentEngine {
    override fun openStream(magnetUri: String, fileIndex: Int): Long {
        val stream = LongArray(1)
        val error = nativeError()
        throwIfFailed(MoviesEngineJni.openStream(handle, magnetUri.encodeToByteArray(), fileIndex, stream, error), error)
        return stream[0]
    }

    override fun streamInfo(stream: Long): StreamInfo {
        val numbers = LongArray(3)
        val texts = arrayOfNulls<ByteArray>(3)
        val error = nativeError()
        throwIfFailed(MoviesEngineJni.streamInfo(handle, stream, numbers, texts, error), error)
        return when (numbers[0]) {
            MOVIES_ENGINE_STREAM_READY -> StreamInfo.Ready(
                url = texts[0].text(),
                fileIndex = numbers[1].toInt(),
                fileSize = numbers[2],
                fileName = texts[1].text()
            )

            MOVIES_ENGINE_STREAM_FAILED -> StreamInfo.Failed(texts[2].text())

            else -> StreamInfo.Resolving
        }
    }

    override fun closeStream(stream: Long) {
        val error = nativeError()
        throwIfFailed(MoviesEngineJni.closeStream(handle, stream, error), error)
    }

    override fun close() {
        if (handle == 0L) return
        MoviesEngineJni.destroy(handle)
        handle = 0L
    }

    /** UTF-8 that is not valid becomes U+FFFD rather than failing the call. */
    private fun ByteArray?.text(): String = this?.decodeToString().orEmpty()
}
