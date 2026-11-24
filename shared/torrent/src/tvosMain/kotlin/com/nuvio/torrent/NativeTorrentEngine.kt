package com.nuvio.torrent

import cnames.structs.movies_engine
import com.nuvio.torrent.native.MOVIES_ENGINE_STREAM_FAILED
import com.nuvio.torrent.native.MOVIES_ENGINE_STREAM_READY
import com.nuvio.torrent.native.movies_engine_close_stream
import com.nuvio.torrent.native.movies_engine_destroy
import com.nuvio.torrent.native.movies_engine_get_stream_info
import com.nuvio.torrent.native.movies_engine_open_stream
import com.nuvio.torrent.native.movies_engine_stream_info
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

/** Owns one native engine handle; [close] releases it once. */
@OptIn(ExperimentalForeignApi::class)
internal class NativeTorrentEngine(
    private var handle: CPointer<movies_engine>?
) : TorrentEngine {
    override fun openStream(magnetUri: String, fileIndex: Int): Long = memScoped {
        val stream = alloc<ULongVar>()
        throwIfFailed(movies_engine_open_stream(handle, magnetUri, fileIndex, stream.ptr))
        stream.value.toLong()
    }

    override fun streamInfo(stream: Long): StreamInfo = memScoped {
        val info = alloc<movies_engine_stream_info>()
        throwIfFailed(movies_engine_get_stream_info(handle, stream.toULong(), info.ptr))
        when (info.state) {
            MOVIES_ENGINE_STREAM_READY -> StreamInfo.Ready(
                url = info.url.toKString(),
                fileIndex = info.file_index,
                fileSize = info.file_size,
                fileName = info.file_name.toKString()
            )

            MOVIES_ENGINE_STREAM_FAILED -> StreamInfo.Failed(info.error.toKString())

            else -> StreamInfo.Resolving
        }
    }

    override fun closeStream(stream: Long) {
        throwIfFailed(movies_engine_close_stream(handle, stream.toULong()))
    }

    override fun close() {
        movies_engine_destroy(handle)
        handle = null
    }
}
